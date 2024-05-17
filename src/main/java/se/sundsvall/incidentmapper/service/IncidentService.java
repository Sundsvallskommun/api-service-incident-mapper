package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.MIN;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.CLOSED;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;
import static se.sundsvall.incidentmapper.service.Constants.JIRA_ISSUE_TITLE_TEMPLATE;
import static se.sundsvall.incidentmapper.service.Constants.JIRA_ISSUE_TYPE;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toAttachmentPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemo;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemoPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toDescription;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toProblemMemo;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toResponsibleGroupPayload;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavaillaz.client.jira.domain.Attachment;
import com.chavaillaz.client.jira.domain.Issue;

import generated.se.sundsvall.pob.PobPayload;
import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;
import se.sundsvall.incidentmapper.integration.jira.JiraIncidentClient;
import se.sundsvall.incidentmapper.integration.pob.POBClient;
import se.sundsvall.incidentmapper.service.mapper.PobMapper;

@Service
@Transactional
public class IncidentService {

	static final List<Status> OPEN_FOR_MODIFICATION_STATUS_LIST = asList(null, SYNCHRONIZED); // Status is only modifiable if current value is one of these.
	static final List<Status> DBCLEAN_ELIGIBLE_FOR_REMOVAL_STATUS_LIST = List.of(CLOSED); // Status is only eligible for removal if one of these during dbcleaner-execution.
	static final Integer DBCLEAN_CLOSED_INCIDENTS_TTL_IN_DAYS = 10;

	private static final Logger LOGGER = LoggerFactory.getLogger(IncidentService.class);
	private static final String LOG_MSG_CLEANING_DELETE_RANGE = "Removing all incidents with modified '{}' (or earlier) and with status matching '{}'.";
	private static final List<String> JIRA_CLOSED_STATUSES = List.of("Closed", "Done", "Won't Do");

	private final IncidentRepository incidentRepository;
	private final JiraIncidentClient jiraIncidentClient;
	private final POBClient pobClient;

	public IncidentService(final IncidentRepository incidentRepository, final JiraIncidentClient jiraClient, final POBClient pobClient) {
		this.incidentRepository = incidentRepository;
		this.jiraIncidentClient = jiraClient;
		this.pobClient = pobClient;
	}

	public void handleIncidentRequest(final IncidentRequest request) {

		final var issueKey = request.getIncidentKey();
		final var incidentEntity = incidentRepository.findByPobIssueKey(issueKey)
			.orElse(IncidentEntity.create().withPobIssueKey(issueKey));

		if (OPEN_FOR_MODIFICATION_STATUS_LIST.contains(incidentEntity.getStatus())) {
			incidentEntity.withStatus(POB_INITIATED_EVENT);
		}

		incidentRepository.saveAndFlush(incidentEntity);
	}

	/**
	 * Poll JIRA for updates on mapped issues.
	 * <p>
	 * All incidents with status "SYNCHRONIZED" (in DB) will be compared with the last-update-timestamp on the Jira-issue.
	 * <p>
	 * If the "last-updated"-timestamp in Jira is greater than the stored synchronization date (lastSynchronizedJira) in DB,
	 * the status will be changed to "JIRA_INITIATED_EVENT". This status will make the issue a candidate for synchronization
	 * towards Pob.
	 */
	public void pollJiraUpdates() {
		incidentRepository.findByStatus(SYNCHRONIZED)
			.forEach(incident -> jiraIncidentClient.getIssue(incident.getJiraIssueKey()).ifPresentOrElse(jiraIssue -> {
				final var lastModifiedJira = jiraIssue.getFields().getUpdated();
				final var lastSynchronizedPob = incident.getLastSynchronizedPob();

				if (anyNull(lastModifiedJira, lastSynchronizedPob)) {
					LOGGER.info("Null dates discovered. lastModifiedJira '{}', lastSynchronizedPob '{}'. Skipping record.", lastModifiedJira, lastSynchronizedPob);
					return;
				}

				if (lastModifiedJira.isAfter(lastSynchronizedPob)) { // Issue has been updated in Jira after last synchronization towards POB.
					LOGGER.info("Updating database. Set status to '{}' on mapping with jiraIssueType '{}'.", JIRA_INITIATED_EVENT, incident.getJiraIssueKey());
					incidentRepository.saveAndFlush(incident.withStatus(JIRA_INITIATED_EVENT));
				}
			}, () -> LOGGER.warn("No jira issue with key '{}' found", incident.getJiraIssueKey())));
	}

	public void cleanObsoleteIncidents() {
		final var statusesEligibleForRemoval = DBCLEAN_ELIGIBLE_FOR_REMOVAL_STATUS_LIST.toArray(Status[]::new);
		final var expiryDate = now(systemDefault()).minusDays(DBCLEAN_CLOSED_INCIDENTS_TTL_IN_DAYS);

		LOGGER.info(LOG_MSG_CLEANING_DELETE_RANGE, expiryDate, statusesEligibleForRemoval);

		incidentRepository.deleteByModifiedBeforeAndStatusIn(expiryDate, statusesEligibleForRemoval);
	}

	public void updateJira() {
		incidentRepository.findByStatus(POB_INITIATED_EVENT)
			.forEach(incident -> {
				if (isBlank(incident.getJiraIssueKey())) {
					createJiraIssue(incident);
					return;
				}
				updateJira(incident);
			});
	}

	public void updateJira(final IncidentEntity incident) {

		// Fetch from POB.
		final var summary = toDescription(pobClient.getCase(incident.getPobIssueKey()).orElse(null));
		final var description = toProblemMemo(pobClient.getProblemMemo(incident.getPobIssueKey()).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(incident.getPobIssueKey()).orElse(null));

		// Fetch from Jira.
		final var jiraIssueKey = incident.getJiraIssueKey();
		final var jiraIssue = jiraIncidentClient.getIssue(jiraIssueKey);

		// Update comments (remove all + add existing from POB).
		jiraIssue.ifPresentOrElse(issue -> {

			// Update issue in Jira
			final var updateIssue = Issue.fromKey(jiraIssueKey);
			updateIssue.getFields().setDescription(description);
			updateIssue.getFields().setSummary(summary);
			jiraIncidentClient.updateIssue(updateIssue);

			// Delete all existing comments in Jira.
			issue.getFields().getComments().stream()
				.forEach(comment -> jiraIncidentClient.deleteComment(jiraIssueKey, comment.getId()));

			// Add new comment (with data from POB).
			jiraIncidentClient.addComment(jiraIssueKey, comments);

			// Save state in DB
			incidentRepository.saveAndFlush(incident
				.withStatus(SYNCHRONIZED)
				.withLastSynchronizedJira(now(systemDefault())));

		}, () -> // Issue is not present in Jira.

		// Save the mapping as POB_INITIATED_EVENT with empty jiraIssueKey (this will trigger a create).
		incidentRepository.saveAndFlush(incident
			.withStatus(POB_INITIATED_EVENT)
			.withJiraIssueKey(null)
			.withLastSynchronizedJira(null)));
	}

	public void createJiraIssue(final IncidentEntity incident) {

		// Fetch from POB.
		final var summary = toDescription(pobClient.getCase(incident.getPobIssueKey()).orElse(null));
		final var description = toProblemMemo(pobClient.getProblemMemo(incident.getPobIssueKey()).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(incident.getPobIssueKey()).orElse(null));

		// Create issue in Jira.
		final var jiraIssueKey = jiraIncidentClient.createIssue(JIRA_ISSUE_TYPE, JIRA_ISSUE_TITLE_TEMPLATE.formatted(summary), description);

		// Add comments in Jira.
		jiraIncidentClient.getIssue(jiraIssueKey).ifPresent(issue -> jiraIncidentClient.addComment(jiraIssueKey, comments));

		// Save state in DB
		incidentRepository.saveAndFlush(incident
			.withStatus(SYNCHRONIZED)
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedJira(now(systemDefault())));
	}

	public void updatePob() {
		incidentRepository.findByStatus(JIRA_INITIATED_EVENT)
			.forEach(entity -> {
				final var jiraIssue = jiraIncidentClient.getIssue(entity.getJiraIssueKey()).orElse(null);
				final var pobAttachments = pobClient.getAttachments(entity.getPobIssueKey()).orElse(null);
				updatePob(entity, jiraIssue, pobAttachments);
			});
	}

	private void updatePob(final IncidentEntity entity, final Issue jiraIssue, final PobPayload pobAttachments) {
		checkJiraStatus(entity, jiraIssue);
		updatePobComment(entity, jiraIssue);
		updatePobDescription(entity, jiraIssue);
		updatePobAttachments(entity, jiraIssue.getFields().getAttachments().getAttachments(), pobAttachments);
		updateJira(entity);
		incidentRepository.saveAndFlush(entity.withLastSynchronizedPob(now(systemDefault())));
	}

	private void checkJiraStatus(final IncidentEntity entity, final Issue jiraIssue) {
		final var statusName = jiraIssue.getFields().getStatus().getName();
		if (JIRA_CLOSED_STATUSES.contains(statusName)) {
			entity.setStatus(CLOSED);
			updatePobUser(entity);
		} else {
			entity.setStatus(SYNCHRONIZED);
		}
	}

	private void updatePobUser(final IncidentEntity entity) {
		pobClient.updateCase(toResponsibleGroupPayload(entity));
	}

	private void updatePobComment(final IncidentEntity entity, final Issue jiraIssue) {
		jiraIssue.getFields().getComments().stream()
			.filter(comment -> comment.getCreated().isAfter(Optional.ofNullable(entity.getLastSynchronizedPob()).orElse(MIN)))
			.filter(comment -> comment.getAuthor() != null)
			.filter(comment -> !comment.getAuthor().getName().equals(jiraIncidentClient.getProperties().username()))
			.forEach(comment -> updatePobWithComment(entity, comment.getBody()));
	}

	private void updatePobWithComment(final IncidentEntity entity, final String comment) {
		pobClient.updateCase(toCaseInternalNotesCustomMemoPayload(entity, comment));
	}

	private void updatePobDescription(final IncidentEntity entity, final Issue jiraIssue) {
		final var pobDescription = toProblemMemo(pobClient.getProblemMemo(entity.getPobIssueKey()).orElse(null));
		final var jiraDescription = jiraIssue.getFields().getDescription();

		if ((jiraDescription != null) && !jiraDescription.equals(pobDescription)) {
			pobClient.updateCase(PobMapper.toDescriptionPayload(entity, jiraDescription));
		}
	}

	private void updatePobAttachments(final IncidentEntity entity, final List<Attachment> jiraAttachments, final PobPayload pobAttachments) {
		jiraAttachments.forEach(jiraAttachment -> updatePobAttachment(entity, pobAttachments, jiraAttachment));
	}

	private void updatePobAttachment(final IncidentEntity entity, final PobPayload pobAttachments, final Attachment jiraAttachment) {
		final boolean attachmentExists = pobAttachments.getLinks().stream()
			.anyMatch(pobAttachment -> pobAttachment.getRelation().equals(jiraAttachment.getFilename()));

		if (!attachmentExists) {
			final var base64String = jiraAttachment.getContent();
			final var payload = toAttachmentPayload(jiraAttachment, base64String);
			pobClient.createAttachment(entity.getPobIssueKey(), payload);
		}
	}
}
