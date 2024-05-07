package se.sundsvall.incidentmapper.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import se.sundsvall.incidentmapper.service.IncidentService;

@Component
public class JiraPollingSchedulerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JiraPollingSchedulerService.class);

	private final IncidentService incidentService;

	public JiraPollingSchedulerService(IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	@Scheduled(cron = "${scheduler.jira-polling.cron:-}")
	@SchedulerLock(name = "jira-polling", lockAtMostFor = "${scheduler.jira-polling.shedlock-lock-at-most-for}")
	public void execute() {

		LOGGER.info("Start polling for Jira updates");

		incidentService.pollJiraUpdates();

		LOGGER.info("End polling for Jira updates");
	}
}
