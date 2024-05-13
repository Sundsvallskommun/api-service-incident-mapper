package se.sundsvall.incidentmapper.service.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.incidentmapper.service.IncidentService;

@ExtendWith(MockitoExtension.class)
class DatabaseCleanerSchedulerServiceTest {

	@Mock
	private IncidentService incidentService;

	@InjectMocks
	private DatabaseCleanerSchedulerService databaseCleanerSchedulerService;

	@Test
	void execute() {

		// Act
		databaseCleanerSchedulerService.execute();

		// Assert
		verify(incidentService).cleanObsoleteIncidents();
		verifyNoMoreInteractions(incidentService);
	}
}
