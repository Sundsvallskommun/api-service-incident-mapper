package se.sundsvall.incidentmapper;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

/**
 * TODO: Future improvements:
 *
 * 1. Remove the Status-enum from persistence layer (and in DB)
 * 2. Add a new entity: Event {id, incidentEntityId, Type [POB_UPDATED, JIRA_UPDATED, JIRA_CLOSED, etc], created
 * 3. Add a List of Event in IncidentEntity (or keep it separated?)
 * 4. Update IncidentService.handleIncidentRequest: Create a new Event with type: POB_UPDATED.
 * 5. Update IncidentService.pollJiraIssues: Will scan ALL mappings and create JIRA_UPDATED and/or JIRA_CLOSED events.
 * 6. Update IncidentService.updateJiraIssues: Fetch all POB_UPDATED-events and perform Jira synchronization.
 * 7. Update IncidentService.updatePobIssues: Fetch all JIRA_UPDATED-events and perform POB synchronization.
 * 8. Update IncidentService.closeIssues: Fetch all JIRA_CLOSED-events and perform close-action.
 * 9. Create a IncidentService.executeSynchronization that calls pollJiraIssues() -> updatePobIssues() -> closeIssues().
 * 10. Update SynchronizerSchedulerService to call IncidentService.executeSynchronization()
 * 10: Rewrite all IT-tests (due to the refactoring above)
 *
 * Question: What will we do with the events after processing? Save event with execute-date and status[EXECUTED] or
 * delete?
 */
@ServiceApplication
@EnableFeignClients
@EnableScheduling
@ExcludeFromJacocoGeneratedCoverageReport
public class Application {
	public static void main(final String... args) {
		run(Application.class, args);
	}
}
