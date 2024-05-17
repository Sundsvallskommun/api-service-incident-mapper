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

		LOGGER.info("Start polling for Jira updates");
		incidentService.pollJiraUpdates();
		LOGGER.info("End polling for Jira updates");

		LOGGER.info("Start Jira updates");
		incidentService.updateJira();
		LOGGER.info("End Jira updates");

		LOGGER.info("Start POB updates");
		incidentService.updatePob();
		LOGGER.info("End POB updates");
	}
}
