package se.sundsvall.incidentmapper.service;

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
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toCaseInternalNotesCustomMemo;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toDescription;
import static se.sundsvall.incidentmapper.service.mapper.PobMapper.toProblemMemo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;
import se.sundsvall.incidentmapper.integration.jira.JiraClient;
import se.sundsvall.incidentmapper.integration.pob.POBClient;

@Service
@Transactional
public class IncidentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(IncidentService.class);

	private static final String LOG_MSG_CLEANING_DELETE_RANGE = "Removing all incidents with modified '{}' (or earlier) and with status matching '{}'.";

	static final List<Status> OPEN_FOR_MODIFICATION_STATUS_LIST = asList(null, SYNCHRONIZED); // Status is only modifiable if current value is one of these.
	static final List<Status> DBCLEAN_ELIGIBLE_FOR_REMOVAL_STATUS_LIST = asList(CLOSED); // Status is is only eligible for removal if one of these during dbcleaner-execution.
	static final Integer DBCLEAN_CLOSED_INCIDENTS_TTL_IN_DAYS = 10;

	private final IncidentRepository incidentRepository;
	private final JiraClient jiraClient;
	private final POBClient pobClient;

	public IncidentService(IncidentRepository incidentRepository, JiraClient jiraClient, POBClient pobClient) {
		this.incidentRepository = incidentRepository;
		this.jiraClient = jiraClient;
		this.pobClient = pobClient;
	}

	public void handleIncidentRequest(IncidentRequest request) {

		final var issueKey = request.getIncidentKey();
		final var incidentEntity = incidentRepository.findByPobIssueKey(issueKey)
			.orElse(IncidentEntity.create().withPobIssueKey(issueKey));

		if (OPEN_FOR_MODIFICATION_STATUS_LIST.contains(incidentEntity.getStatus())) {
			incidentEntity.withStatus(POB_INITIATED_EVENT);
		}

		incidentRepository.save(incidentEntity);
	}

	/**
	 * Poll JIRA for for updates on mapped issues.
	 *
	 * All incidents with status "SYNCHRONIZED" (in DB) will be compared with the last-update-timestamp on the Jira-issue.
	 *
	 * If the "last-updated"-timestamp in Jira is greater than the stored synchronization date (lastSynchronizedJira) in DB,
	 * the status will be changed to "JIRA_INITIATED_EVENT". This status will make the issue a candidate for synchronization
	 * towards POB.
	 */
	public void pollJiraUpdates() {
		incidentRepository.findByStatus(SYNCHRONIZED).stream()
			.forEach(incident -> jiraClient.getIssue(incident.getJiraIssueKey()).ifPresentOrElse(jiraIssue -> {
				final var lastModifiedJira = toOffsetDateTime(jiraIssue.getUpdateDate());
				final var lastSynchronizedJira = incident.getLastSynchronizedJira();

				if (anyNull(lastModifiedJira, lastSynchronizedJira)) {
					LOGGER.info("Null dates discovered. lastModifiedJira '{}', lastSynchronizedJira '{}'. Skipping record.", lastModifiedJira, lastSynchronizedJira);
					return;
				}

				if (lastModifiedJira.isAfter(lastSynchronizedJira)) { // Issue has been updated in Jira after last synchronization.
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
		incidentRepository.findByStatus(POB_INITIATED_EVENT).stream()
			.forEach(incident -> {
				if (isBlank(incident.getJiraIssueKey())) {
					createJiraIssue(incident);
				}
			});
	}

	private void createJiraIssue(IncidentEntity incident) {

		final var summary = toDescription(pobClient.getCase(incident.getPobIssueKey()));
		final var description = toProblemMemo(pobClient.getProblemMemo(incident.getPobIssueKey()).orElse(null));
		final var comments = toCaseInternalNotesCustomMemo(pobClient.getCaseInternalNotesCustom(incident.getPobIssueKey()).orElse(null));

		// Create issue.
		final var jiraIssueKey = jiraClient.createIssue(JIRA_ISSUE_TYPE, JIRA_ISSUE_TITLE_TEMPLATE.formatted(summary), description);

		// Add comments.
		jiraClient.getIssue(jiraIssueKey).ifPresent(issue -> jiraClient.addComment(issue, comments));

		incidentRepository.save(incident
			.withStatus(SYNCHRONIZED)
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedJira(now(systemDefault())));
	}

	private OffsetDateTime toOffsetDateTime(DateTime jodaDateTime) {
		return Optional.ofNullable(jodaDateTime)
			.map(joda -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(jodaDateTime.getMillis()), ZoneId.of(jodaDateTime.getZone().getID())))
			.orElse(null);
	}
}
