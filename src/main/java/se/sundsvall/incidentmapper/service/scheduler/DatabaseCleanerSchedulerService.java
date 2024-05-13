package se.sundsvall.incidentmapper.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import se.sundsvall.incidentmapper.service.IncidentService;

@Component
public class DatabaseCleanerSchedulerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCleanerSchedulerService.class);

	private static final String LOG_CLEANING_STARTED = "Beginning removal of obsolete entities in the database.";
	private static final String LOG_CLEANING_ENDED = "Cleaning of obsolete entities in database has ended.";

	private final IncidentService incidentService;

	public DatabaseCleanerSchedulerService(IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	@Scheduled(cron = "${scheduler.dbcleaner.cron:-}")
	@SchedulerLock(name = "dbcleaner", lockAtMostFor = "${scheduler.dbcleaner.shedlock-lock-at-most-for}")
	public void execute() {

		LOGGER.info(LOG_CLEANING_STARTED);
		incidentService.cleanObsoleteIncidents();
		LOGGER.info(LOG_CLEANING_ENDED);
	}
}
