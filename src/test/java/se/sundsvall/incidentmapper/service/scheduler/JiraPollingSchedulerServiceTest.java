package se.sundsvall.incidentmapper.service.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.incidentmapper.service.IncidentService;

@ExtendWith(MockitoExtension.class)
class JiraPollingSchedulerServiceTest {

	@Mock
	private IncidentService incidentService;

	@InjectMocks
	private JiraPollingSchedulerService jiraPollingSchedulerService;

	@Test
	void execute() {

		// Act
		jiraPollingSchedulerService.execute();

		// Assert
		verify(incidentService).pollJiraUpdates();
	}
}
