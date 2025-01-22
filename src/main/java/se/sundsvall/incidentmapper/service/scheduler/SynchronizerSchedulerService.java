package se.sundsvall.incidentmapper.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.incidentmapper.service.IncidentService;

@Component
public class SynchronizerSchedulerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizerSchedulerService.class);

	private final IncidentService incidentService;

	public SynchronizerSchedulerService(final IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	@Dept44Scheduled(
		cron = "${scheduler.synchronizer.cron:-}",
		name = "${scheduler.synchronizer.name}",
		lockAtMostFor = "${scheduler.synchronizer.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.synchronizer.maximum-execution-time}")
	public void execute() {

		LOGGER.info("Start polling for Jira modifications");
		incidentService.pollJiraIssues();
		LOGGER.info("End polling for Jira modifications");

		LOGGER.info("Start POB synchronization");
		incidentService.updatePobIssues();
		LOGGER.info("End POB synchronization");

		LOGGER.info("Start Jira synchronization");
		incidentService.updateJiraIssues();
		LOGGER.info("End Jira synchronization");

		LOGGER.info("Start close issues");
		incidentService.closeIssues();
		LOGGER.info("End close issues");
	}
}
