package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.MIN;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.util.FileSystemUtils.deleteRecursively;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toAttachmentPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemo;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemoPayload;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toDescription;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toFormattedMail;
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
import se.sundsvall.incidentmapper.integration.pob.model.Mail;
import se.sundsvall.incidentmapper.service.configuration.SynchronizationProperties;
import se.sundsvall.incidentmapper.service.mapper.PobMapper;

@Service
@Transactional
public class IncidentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(IncidentService.class);

	private static final List<Status> OPEN_FOR_MODIFICATION_STATUS_LIST = asList(SYNCHRONIZED); // Status is only modifiable if current value is one of these.
	private static final List<String> JIRA_CLOSED_STATUSES = List.of("Closed", "Done", "Review done", "Resolved", "Won't do");
	private static final List<String> JIRA_ISSUE_LABELS = List.of("support-ticket");
	private static final String JIRA_ISSUE_CREATED = "A new Jira issue has been created for you: %s/browse/%s";
	private static final String JIRA_ISSUE_TYPE = "Bug";
	private static final String JIRA_TODO_STATUS = "To Do";
	private static final String JIRA_ISSUE_TITLE_TEMPLATE = "Supportärende %s (%s)";
	private static final String APPLICATION_TEMP_FOLDER_PATH_TEMPLATE = "%s/%s/%s";

	private final IncidentRepository incidentRepository;
	private final JiraIncidentClient jiraIncidentClient;
	private final POBClient pobClient;
	private final SynchronizationProperties synchronizationProperties;
	private final SlackService slackService;

	public IncidentService(
		IncidentRepository incidentRepository,
		JiraIncidentClient jiraClient,
		POBClient pobClient,
		SynchronizationProperties synchronizationProperties,
		SlackService slackService) {

		this.incidentRepository = incidentRepository;
		this.jiraIncidentClient = jiraClient;
		this.pobClient = pobClient;
		this.synchronizationProperties = synchronizationProperties;
		this.slackService = slackService;
	}

	/**
	 * Takes an IncidentRequest and map it to an IncidentEntity in the database, with status POB_INITIATED_EVENT.
	 *
	 * @param municipalityId  the municipalityId.
	 * @param incidentRequest the request (from POB).
	 */
	public synchronized void handleIncidentRequest(final String municipalityId, final IncidentRequest incidentRequest) {
		final var issueKey = incidentRequest.getIncidentKey();
		final var incidentEntity = incidentRepository.findByMunicipalityIdAndPobIssueKey(municipalityId, issueKey)
			.orElse(IncidentEntity.create()
				.withMunicipalityId(municipalityId)
				.withPobIssueKey(issueKey)
				.withStatus(POB_INITIATED_EVENT));

		// Only set the status to POB_INITIATED_EVENT if status is currently SYNCHRONIZED.
		if (OPEN_FOR_MODIFICATION_STATUS_LIST.contains(incidentEntity.getStatus())) {
			incidentEntity.withStatus(POB_INITIATED_EVENT);
		}

		incidentRepository.saveAndFlush(incidentEntity);
	}

	/**
	 * Search for all issues (mappings) that have a closed Jira-issue (See definition of closed in: JIRA_CLOSED_STATUSES).
	 *
	 * All closed Jira-issues will have the corresponding POB-issue assigned back to first line and then the mapping will be
	 * deleted.
	 */
	public void closeIssues() {
		incidentRepository.findAll().stream()
			.filter(incidentEntity -> isNotBlank(incidentEntity.getJiraIssueKey()))
			.forEach(incidentEntity -> jiraIncidentClient.getIssue(incidentEntity.getJiraIssueKey()).ifPresent(jiraIssue -> {
				final var statusName = jiraIssue.getFields().getStatus().getName();
				final var doCloseIssue = JIRA_CLOSED_STATUSES.stream().anyMatch(status -> equalsIgnoreCase(status, statusName));

				LOGGER.info("Issue: '{}' has status: '{}'. Issue will be closed: '{}'", incidentEntity.getJiraIssueKey(), statusName, doCloseIssue);

				// Issue is processed in Jira.
				if (doCloseIssue) {
					updatePobUser(incidentEntity);
					incidentRepository.delete(incidentEntity);
				}
			}));
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
	public void pollJiraIssues() {
		incidentRepository.findByStatus(SYNCHRONIZED)
			.forEach(incidentEntity -> jiraIncidentClient.getIssue(incidentEntity.getJiraIssueKey()).ifPresentOrElse(jiraIssue -> {
				final var lastModifiedJira = Optional.ofNullable(jiraIssue.getFields().getUpdated()).orElse(MIN);
				final var lastSynchronizedJira = Optional.ofNullable(incidentEntity.getLastSynchronizedJira()).orElse(MIN);

				if (lastModifiedJira.isAfter(lastSynchronizedJira.plusSeconds(synchronizationProperties.clockSkewInSeconds()))) {
					// Issue has been updated in Jira after last synchronization towards Jira.
					LOGGER.info("Set status to '{}' on mapping with jiraIssueType '{}'.", JIRA_INITIATED_EVENT, incidentEntity.getJiraIssueKey());
					incidentRepository.saveAndFlush(incidentEntity.withStatus(JIRA_INITIATED_EVENT));
				}
			}, () -> {
				LOGGER.warn("No jira issue with key '{}' found. Creating new Jira-issue and updating incident mapping in DB...", incidentEntity.getJiraIssueKey());

				// Issue does not exist. Save the mapping as POB_INITIATED_EVENT with empty jiraIssueKey (this will trigger a create).
				incidentRepository.saveAndFlush(incidentEntity
					.withStatus(POB_INITIATED_EVENT)
					.withJiraIssueKey(null)
					.withLastSynchronizedJira(null));
			}));
	}

	public void updateJiraIssues() {
		incidentRepository.findByStatus(POB_INITIATED_EVENT)
			.forEach(incidentEntity -> {
				if (isBlank(incidentEntity.getJiraIssueKey())) {
					createJiraIssue(incidentEntity);
					return;
				}
				updateJiraIssue(incidentEntity);
			});
	}

	private void updateJiraIssue(final IncidentEntity incidentEntity) {

		// Fetch from POB.
		final var pobIssueKey = incidentEntity.getPobIssueKey();
		final var summary = toDescription(pobClient.getCase(pobIssueKey).orElse(null));
		final var description = toProblemMemo(pobClient.getProblemMemo(pobIssueKey).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(pobIssueKey).orElse(null));

		// Fetch from Jira.
		final var jiraIssueKey = incidentEntity.getJiraIssueKey();
		final var jiraIssue = jiraIncidentClient.getIssue(jiraIssueKey);

		jiraIssue.ifPresentOrElse(issue -> {

			// Update issue in Jira
			final var updateIssue = Issue.fromKey(jiraIssueKey);
			updateIssue.getFields().setDescription(description);
			updateIssue.getFields().setSummary(JIRA_ISSUE_TITLE_TEMPLATE.formatted(pobIssueKey, summary));
			jiraIncidentClient.updateIssue(updateIssue);

			// Delete all existing comments in Jira.
			issue.getFields().getComments()
				.forEach(comment -> jiraIncidentClient.deleteComment(jiraIssueKey, comment.getId()));

			// Delete all attachments in Jira.
			issue.getFields().getAttachments()
				.forEach(attachment -> jiraIncidentClient.deleteAttachment(attachment.getId()));

			// Add POB mails to Jira (as comments and attachments).
			getPobMails(incidentEntity).forEach(mail -> {
				jiraIncidentClient.addComment(jiraIssueKey, toFormattedMail(mail));
				Optional.ofNullable(mail.getAttachments()).orElse(emptyList())
					.forEach(attachment -> jiraIncidentClient.addAttachment(jiraIssueKey, attachment));
			});

			// Add case attachments.
			getPobAttachments(incidentEntity).forEach(attachmentFile -> jiraIncidentClient.addAttachment(jiraIssueKey, attachmentFile));

			// Add new comment (with data from POB).
			jiraIncidentClient.addComment(jiraIssueKey, comments);

			// Clean temp-dir.
			removeFilesInTempFolder();

			LOGGER.info("Issue '{}' synchronized in Jira", jiraIssueKey);

			// Save state in DB
			incidentRepository.saveAndFlush(incidentEntity
				.withStatus(SYNCHRONIZED)
				.withLastSynchronizedJira(now(systemDefault())));

		}, () -> // Issue is not present in Jira.

		// Issue does not exist in Jira. Save the mapping as POB_INITIATED_EVENT with empty jiraIssueKey (this will trigger a
		// create).
		incidentRepository.saveAndFlush(incidentEntity
			.withStatus(POB_INITIATED_EVENT)
			.withJiraIssueKey(null)
			.withLastSynchronizedJira(null)));
	}

	private void createJiraIssue(final IncidentEntity incidentEntity) {

		// Fetch from POB.
		final var pobIssueKey = incidentEntity.getPobIssueKey();
		final var summary = toDescription(pobClient.getCase(pobIssueKey).orElse(null));
		final var description = toProblemMemo(pobClient.getProblemMemo(pobIssueKey).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(pobIssueKey).orElse(null));

		// Create issue in Jira.
		final var jiraIssueKey = jiraIncidentClient.createIssue(JIRA_ISSUE_TYPE, JIRA_ISSUE_LABELS, JIRA_ISSUE_TITLE_TEMPLATE.formatted(pobIssueKey, summary), description);
		final var jiraIssue = jiraIncidentClient.getIssue(jiraIssueKey);

		jiraIssue.ifPresent(issue -> {

			// Set initial status on issue in Jira.
			Optional.ofNullable(jiraIncidentClient.getTransitions(jiraIssueKey).get(JIRA_TODO_STATUS)).ifPresent(initialStatus -> {
				jiraIncidentClient.performTransition(jiraIssueKey, initialStatus);
				LOGGER.info("Updated initial status on issue '{}' to '{}'", jiraIssueKey, initialStatus.getName());
			});

			// Add POB mails to Jira (as comments and attachments).
			getPobMails(incidentEntity).forEach(mail -> {
				jiraIncidentClient.addComment(jiraIssueKey, toFormattedMail(mail));
				Optional.ofNullable(mail.getAttachments()).orElse(emptyList())
					.forEach(attachment -> jiraIncidentClient.addAttachment(jiraIssueKey, attachment));
			});

			// Add comments in Jira.
			jiraIncidentClient.addComment(jiraIssueKey, comments);

			// Add case attachments.
			getPobAttachments(incidentEntity).forEach(attachment -> jiraIncidentClient.addAttachment(jiraIssueKey, attachment));

			// Clean temp-dir.
			removeFilesInTempFolder();

			LOGGER.info("Issue '{}' created in Jira", jiraIssueKey);

			// Save state in DB.
			incidentRepository.saveAndFlush(incidentEntity
				.withStatus(SYNCHRONIZED)
				.withJiraIssueKey(jiraIssueKey)
				.withLastSynchronizedJira(now(systemDefault())));

			// Send Slack notification.
			slackService.sendToSlack(incidentEntity.getMunicipalityId(), JIRA_ISSUE_CREATED.formatted(jiraIncidentClient.getProperties().url(), jiraIssueKey));
		});
	}

	public void updatePobIssues() {
		incidentRepository.findByStatus(JIRA_INITIATED_EVENT)
			.forEach(incidentEntity -> {
				final var jiraIssue = jiraIncidentClient.getIssue(incidentEntity.getJiraIssueKey()).orElse(null);
				final var pobAttachments = pobClient.getAttachments(incidentEntity.getPobIssueKey()).orElse(null);
				updatePob(incidentEntity, jiraIssue, pobAttachments);
			});
	}

	private void updatePob(final IncidentEntity incidentEntity, final Issue jiraIssue, final PobPayload pobAttachments) {
		updatePobComment(incidentEntity, jiraIssue);
		updatePobDescription(incidentEntity, jiraIssue);
		updatePobAttachments(incidentEntity, jiraIssue.getFields().getAttachments().getAttachments(), pobAttachments);
		updateJiraIssue(incidentEntity);

		incidentRepository.saveAndFlush(incidentEntity
			.withStatus(SYNCHRONIZED)
			.withLastSynchronizedPob(now(systemDefault())));

		LOGGER.info("Issue '{}' synchronized in POB", incidentEntity.getPobIssueKey());
	}

	private void updatePobUser(final IncidentEntity incidentEntity) {
		pobClient.updateCase(toResponsibleGroupPayload(incidentEntity.getPobIssueKey(), synchronizationProperties.responsibleUserGroupInPob()));
	}

	private void updatePobComment(final IncidentEntity incidentEntity, final Issue jiraIssue) {
		jiraIssue.getFields().getComments().stream()
			.filter(comment -> comment.getCreated().isAfter(Optional.ofNullable(incidentEntity.getLastSynchronizedPob()).orElse(MIN).plusSeconds(synchronizationProperties.clockSkewInSeconds())))
			.filter(comment -> comment.getAuthor() != null)
			.filter(comment -> !comment.getAuthor().getName().equals(jiraIncidentClient.getProperties().username()))
			.forEach(comment -> updatePobWithComment(incidentEntity, comment.getAuthor().getDisplayName() + ":\n " + comment.getBody()));
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
		final boolean attachmentExists = Optional.ofNullable(pobAttachments).orElse(new PobPayload()).getLinks().stream()
			.anyMatch(pobAttachment -> Objects.equals(pobAttachment.getRelation(), jiraAttachment.getFilename()));

		if (!attachmentExists) {
			final var base64String = jiraIncidentClient.getAttachment(jiraAttachment.getContent());
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
					var attachmentFileName = link.getRelation();
					final var href = link.getHref();
					final var attachmentId = href.substring(href.lastIndexOf("/") + 1);
					try {
						final var attachmentResponse = pobClient.getAttachment(incidentEntity.getPobIssueKey(), attachmentId);
						final var contentType = attachmentResponse.getHeaders().getContentType();
						if (!attachmentFileName.contains(".") && nonNull(contentType)) {
							// Attachment doesn't have a suffix, use contentType mime subtype instead.
							attachmentFileName += "." + contentType.getSubtype();
						}
						final var file = new File(APPLICATION_TEMP_FOLDER_PATH_TEMPLATE.formatted(synchronizationProperties.tempFolder(), incidentEntity.getPobIssueKey(), attachmentFileName));
						copyInputStreamToFile(attachmentResponse.getBody().getInputStream(), file);

						return file;
					} catch (final IOException e) {
						LOGGER.error("Problem fetching attachment binary data from POB", e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toList())
			.orElse(emptyList());
	}

	private List<Mail> getPobMails(final IncidentEntity incidentEntity) {
		return pobClient.getReceivedMailIds(incidentEntity.getPobIssueKey()).stream()
			.map(payLoad -> pobClient.getMail((String) payLoad.getData().get("Id")).orElse(null))
			.filter(Objects::nonNull)
			.map(PobMapper::toMail)
			.filter(Objects::nonNull)
			.toList();
	}

	private void removeFilesInTempFolder() {
		asList(new File(synchronizationProperties.tempFolder()).listFiles())
			.forEach(file -> {
				LOGGER.info("Delete file: {}", file.getAbsolutePath());
				deleteRecursively(file);
			});
	}
}
