package se.sundsvall.incidentmapper.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import se.sundsvall.incidentmapper.service.IncidentService;

@Component
public class SynchronizerSchedulerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizerSchedulerService.class);

	private final IncidentService incidentService;

	public SynchronizerSchedulerService(final IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	@Scheduled(cron = "${scheduler.synchronizer.cron:-}")
	@SchedulerLock(name = "jira-polling", lockAtMostFor = "${scheduler.synchronizer.shedlock-lock-at-most-for}")
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
