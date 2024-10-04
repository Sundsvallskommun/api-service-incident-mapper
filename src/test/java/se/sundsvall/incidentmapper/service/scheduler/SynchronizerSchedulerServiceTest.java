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
class SynchronizerSchedulerServiceTest {

	@Mock
	private IncidentService incidentService;

	@InjectMocks
	private SynchronizerSchedulerService synchronizerSchedulerService;

	@Test
	void execute() {

		// Act
		synchronizerSchedulerService.execute();

		// Assert
		verify(incidentService).pollJiraIssues();
		verify(incidentService).updatePobIssues();
		verify(incidentService).updateJiraIssues();
		verify(incidentService).closeIssues();
		verifyNoMoreInteractions(incidentService);
	}
}
