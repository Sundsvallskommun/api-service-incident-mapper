package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.MIN;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.CLOSED;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toAttachmentPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemo;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemoPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toDescription;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toProblemMemo;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toProblemPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toResponsibleGroupPayload;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
import se.sundsvall.incidentmapper.service.configuration.SynchronizationProperties;

@Service
@Transactional
public class IncidentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(IncidentService.class);

	private static final List<Status> OPEN_FOR_MODIFICATION_STATUS_LIST = asList(null, SYNCHRONIZED); // Status is only modifiable if current value is one of these.
	private static final List<Status> DBCLEAN_ELIGIBLE_FOR_REMOVAL_STATUS_LIST = List.of(CLOSED); // Status is only eligible for removal if one of these during dbcleaner-execution.
	private static final Integer DBCLEAN_CLOSED_INCIDENTS_TTL_IN_DAYS = 10;
	private static final List<String> JIRA_CLOSED_STATUSES = List.of("Closed", "Done", "Resolved", "Won't Do", "wont-do");
	private static final String JIRA_TODO_STATUS = "To Do";

	private static final String JIRA_ISSUE_TYPE = "Bug";
	private static final String JIRA_ISSUE_TITLE_TEMPLATE = "Supportärende (%s).";
	private static final String APPLICATION_TEMP_FOLDER_PATH_TEMPLATE = "%s/%s";

	private final IncidentRepository incidentRepository;
	private final JiraIncidentClient jiraIncidentClient;
	private final POBClient pobClient;
	private final SynchronizationProperties synchronizationProperties;

	public IncidentService(final IncidentRepository incidentRepository, final JiraIncidentClient jiraClient, final POBClient pobClient, SynchronizationProperties synchronizationProperties) {
		this.incidentRepository = incidentRepository;
		this.jiraIncidentClient = jiraClient;
		this.pobClient = pobClient;
		this.synchronizationProperties = synchronizationProperties;
	}

	/**
	 * Takes an IncidentRequest and map it to an IncidentEntity in the database, with status POB_INITIATED_EVENT.
	 *
	 * @param incidentRequest the request (from POB).
	 */
	public void handleIncidentRequest(final IncidentRequest incidentRequest) {

		final var issueKey = incidentRequest.getIncidentKey();
		final var incidentEntity = incidentRepository.findByPobIssueKey(issueKey)
			.orElse(IncidentEntity.create().withPobIssueKey(issueKey));

		// Only set the status to POB_INITIATED_EVENT if status is currently SYNCHRONIZED.
		if (OPEN_FOR_MODIFICATION_STATUS_LIST.contains(incidentEntity.getStatus())) {
			incidentEntity.withStatus(POB_INITIATED_EVENT);
			incidentRepository.saveAndFlush(incidentEntity);
		}
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
			.forEach(incidentEntity -> jiraIncidentClient.getIssue(incidentEntity.getJiraIssueKey()).ifPresentOrElse(jiraIssue -> {
				final var lastModifiedJira = Optional.ofNullable(jiraIssue.getFields().getUpdated()).orElse(MIN);
				final var lastSynchronizedJira = Optional.ofNullable(incidentEntity.getLastSynchronizedJira()).orElse(MIN);

				if (lastModifiedJira.isAfter(lastSynchronizedJira.plusSeconds(synchronizationProperties.clockSkewInSeconds()))) {
					// Issue has been updated in Jira after last synchronization towards Jira.
					LOGGER.info("Set status to '{}' on mapping with jiraIssueType '{}'.", JIRA_INITIATED_EVENT, incidentEntity.getJiraIssueKey());
					incidentRepository.saveAndFlush(incidentEntity.withStatus(JIRA_INITIATED_EVENT));
				}
			}, () -> LOGGER.warn("No jira issue with key '{}' found", incidentEntity.getJiraIssueKey())));
	}

	/**
	 * Removes obsolete records from the database.
	 *
	 * Records are eligible for cleaning if these conditions are fulfilled:
	 * - 'IncidentEntity.status' is one of the statuses defined in DBCLEAN_ELIGIBLE_FOR_REMOVAL_STATUS_LIST.
	 * - 'IncidentEntity.modified' is a date older than the number of days defined in DBCLEAN_CLOSED_INCIDENTS_TTL_IN_DAYS.
	 *
	 */
	public void cleanObsoleteIncidents() {
		final var statusesEligibleForRemoval = DBCLEAN_ELIGIBLE_FOR_REMOVAL_STATUS_LIST.toArray(Status[]::new);
		final var expiryDate = now(systemDefault()).minusDays(DBCLEAN_CLOSED_INCIDENTS_TTL_IN_DAYS);

		LOGGER.info("Removing all incidents with modified '{}' (or earlier) and with status matching '{}'.", expiryDate, statusesEligibleForRemoval);

		incidentRepository.deleteByModifiedBeforeAndStatusIn(expiryDate, statusesEligibleForRemoval);
	}

	public void updateJira() {
		incidentRepository.findByStatus(POB_INITIATED_EVENT)
			.forEach(incidentEntity -> {
				if (isBlank(incidentEntity.getJiraIssueKey())) {
					createJiraIssue(incidentEntity);
					return;
				}
				updateJiraIssue(incidentEntity);
			});
	}

	public void updateJiraIssue(final IncidentEntity incidentEntity) {

		// Fetch from POB.
		final var summary = toDescription(pobClient.getCase(incidentEntity.getPobIssueKey()).orElse(null));
		final var description = toProblemMemo(pobClient.getProblemMemo(incidentEntity.getPobIssueKey()).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(incidentEntity.getPobIssueKey()).orElse(null));

		// Fetch from Jira.
		final var jiraIssueKey = incidentEntity.getJiraIssueKey();
		final var jiraIssue = jiraIncidentClient.getIssue(jiraIssueKey);

		// Update comments (remove all + add existing from POB).
		jiraIssue.ifPresentOrElse(issue -> {

			// Update issue in Jira
			final var updateIssue = Issue.fromKey(jiraIssueKey);
			updateIssue.getFields().setDescription(description);
			updateIssue.getFields().setSummary(summary);

			// Update status (if closed).
			if (JIRA_CLOSED_STATUSES.contains(issue.getFields().getStatus().getName())) {
				updateIssue.getFields().setStatus(com.chavaillaz.client.jira.domain.Status.fromName(JIRA_TODO_STATUS));
			}

			jiraIncidentClient.updateIssue(updateIssue);

			// Delete all existing comments in Jira.
			issue.getFields().getComments().stream()
				.forEach(comment -> jiraIncidentClient.deleteComment(jiraIssueKey, comment.getId()));

			// Add new comment (with data from POB).
			jiraIncidentClient.addComment(jiraIssueKey, comments);

			// Delete all attachments in Jira.
			issue.getFields().getAttachments().stream()
				.forEach(attachment -> jiraIncidentClient.deleteAttachment(attachment.getId()));

			// Add attachments.
			getPobAttachments(incidentEntity).forEach(attachment -> jiraIncidentClient.addAttachment(jiraIssueKey, attachment));

			// Clean temp-dir.
			removeFilesInTempFilder();

			LOGGER.info("Issue '{}' synchronized in Jira", jiraIssueKey);

			// Save state in DB
			incidentRepository.saveAndFlush(incidentEntity
				.withStatus(SYNCHRONIZED)
				.withLastSynchronizedJira(now(systemDefault())));

		}, () -> // Issue is not present in Jira.

		// Save the mapping as POB_INITIATED_EVENT with empty jiraIssueKey (this will trigger a create).
		incidentRepository.saveAndFlush(incidentEntity
			.withStatus(POB_INITIATED_EVENT)
			.withJiraIssueKey(null)
			.withLastSynchronizedJira(null)));
	}

	public void createJiraIssue(final IncidentEntity incidentEntity) {

		// Fetch from POB.
		final var summary = toDescription(pobClient.getCase(incidentEntity.getPobIssueKey()).orElse(null));
		final var description = toProblemMemo(pobClient.getProblemMemo(incidentEntity.getPobIssueKey()).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(incidentEntity.getPobIssueKey()).orElse(null));

		// Create issue in Jira.
		final var jiraIssueKey = jiraIncidentClient.createIssue(JIRA_ISSUE_TYPE, JIRA_ISSUE_TITLE_TEMPLATE.formatted(summary), description);
		final var jiraIssue = jiraIncidentClient.getIssue(jiraIssueKey);

		jiraIssue.ifPresent(issue -> {

			// Add comments in Jira.
			jiraIncidentClient.addComment(jiraIssueKey, comments);

			// Add attachments.
			getPobAttachments(incidentEntity).forEach(attachment -> jiraIncidentClient.addAttachment(jiraIssueKey, attachment));

			// Clean temp-dir.
			removeFilesInTempFilder();

			LOGGER.info("Issue '{}' created in Jira", jiraIssueKey);
		});

		// Save state in DB
		incidentRepository.saveAndFlush(incidentEntity
			.withStatus(SYNCHRONIZED)
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedJira(now(systemDefault())));
	}

	public void updatePob() {
		incidentRepository.findByStatus(JIRA_INITIATED_EVENT)
			.forEach(incidentEntity -> {
				final var jiraIssue = jiraIncidentClient.getIssue(incidentEntity.getJiraIssueKey()).orElse(null);
				final var pobAttachments = pobClient.getAttachments(incidentEntity.getPobIssueKey()).orElse(null);
				updatePob(incidentEntity, jiraIssue, pobAttachments);
			});
	}

	private void updatePob(final IncidentEntity incidentEntity, final Issue jiraIssue, final PobPayload pobAttachments) {
		checkJiraStatus(incidentEntity, jiraIssue);
		updatePobComment(incidentEntity, jiraIssue);
		updatePobDescription(incidentEntity, jiraIssue);
		updatePobAttachments(incidentEntity, jiraIssue.getFields().getAttachments().getAttachments(), pobAttachments);
		updateJiraIssue(incidentEntity);
		incidentRepository.saveAndFlush(incidentEntity.withLastSynchronizedPob(now(systemDefault())));
	}

	private void checkJiraStatus(final IncidentEntity incidentEntity, final Issue jiraIssue) {
		final var statusName = jiraIssue.getFields().getStatus().getName();
		if (JIRA_CLOSED_STATUSES.contains(statusName)) {
			updatePobUser(incidentEntity.withStatus(CLOSED));
		} else {
			incidentEntity.withStatus(SYNCHRONIZED);
		}
	}

	private void updatePobUser(final IncidentEntity incidentEntity) {
		pobClient.updateCase(toResponsibleGroupPayload(incidentEntity, synchronizationProperties.responsibleUserGroupInPob()));
	}

	private void updatePobComment(final IncidentEntity incidentEntity, final Issue jiraIssue) {
		jiraIssue.getFields().getComments().stream()
			.filter(comment -> comment.getCreated().isAfter(Optional.ofNullable(incidentEntity.getLastSynchronizedPob()).orElse(MIN).plusSeconds(synchronizationProperties.clockSkewInSeconds())))
			.filter(comment -> comment.getAuthor() != null)
			.filter(comment -> !comment.getAuthor().getName().equals(jiraIncidentClient.getProperties().username()))
			.forEach(comment -> updatePobWithComment(incidentEntity, comment.getBody()));
	}

	private void updatePobWithComment(final IncidentEntity incidentEntity, final String comment) {
		pobClient.updateCase(toCaseInternalNotesCustomMemoPayload(incidentEntity, comment));
	}

	private void updatePobDescription(final IncidentEntity incidentEntity, final Issue jiraIssue) {
		final var pobDescription = toProblemMemo(pobClient.getProblemMemo(incidentEntity.getPobIssueKey()).orElse(null));
		final var jiraDescription = jiraIssue.getFields().getDescription();

		if ((jiraDescription != null) && !jiraDescription.equals(pobDescription)) {
			pobClient.updateCase(toProblemPayload(incidentEntity, jiraDescription));
		}
	}

	private void updatePobAttachments(final IncidentEntity incidentEntity, final List<Attachment> jiraAttachments, final PobPayload pobAttachments) {
		jiraAttachments.forEach(jiraAttachment -> updatePobAttachment(incidentEntity, pobAttachments, jiraAttachment));
	}

	private void updatePobAttachment(final IncidentEntity incidentEntity, final PobPayload pobAttachments, final Attachment jiraAttachment) {
		final boolean attachmentExists = pobAttachments.getLinks().stream()
			.anyMatch(pobAttachment -> Objects.equals(pobAttachment.getRelation(), jiraAttachment.getFilename()));

		if (!attachmentExists) {
			final var base64String = jiraAttachment.getContent();
			final var payload = toAttachmentPayload(jiraAttachment, base64String);
			pobClient.createAttachment(incidentEntity.getPobIssueKey(), payload);
		}
	}

	private List<File> getPobAttachments(final IncidentEntity incidentEntity) {
		return pobClient.getAttachments(incidentEntity.getPobIssueKey())
			.map(attachment -> attachment.getLinks().stream()
				.filter(link -> isNotEmpty(link.getRelation()))
				.filter(link -> isNotEmpty(link.getHref()))
				.map(link -> {
					final var attachmentFileName = link.getRelation();
					final var href = link.getHref();
					final var attachmentId = href.substring(href.lastIndexOf("/") + 1);
					final var file = new File(APPLICATION_TEMP_FOLDER_PATH_TEMPLATE.formatted(synchronizationProperties.tempFolder(), attachmentFileName));

					try {
						copyInputStreamToFile(pobClient.getAttachment(incidentEntity.getPobIssueKey(), attachmentId).getInputStream(), file);
					} catch (final IOException e) {
						LOGGER.error("Problem fetching attachment binary data from POB", e);
					}

					return file;
				})
				.toList())
			.orElse(emptyList());
	}

	private void removeFilesInTempFilder() {
		asList(new File(synchronizationProperties.tempFolder()).listFiles())
			.forEach(File::delete);
	}
}
