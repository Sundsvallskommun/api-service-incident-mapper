package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

	@Mock
	private IncidentRepository incidentRepositoryMock;

	@InjectMocks
	private IncidentService incidentService;

	@Captor
	private ArgumentCaptor<IncidentEntity> incidentEntityCaptor;

	@Test
	void handleIncidentRequestNew() {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var incidentRequest = IncidentRequest.create()
			.withIncidentKey(pobIssueKey);

		when(incidentRepositoryMock.findByPobIssueKey(pobIssueKey)).thenReturn(empty());

		// Act
		incidentService.handleIncidentRequest(incidentRequest);

		// Assert
		verify(incidentRepositoryMock).findByPobIssueKey(pobIssueKey);
		verify(incidentRepositoryMock).save(incidentEntityCaptor.capture());

		final var capturedValue = incidentEntityCaptor.getValue();
		assertThat(capturedValue.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedValue.getPobIssueLastModified()).isCloseTo(now(), within(2, SECONDS));
		assertThat(capturedValue.getStatus()).isEqualTo(POB_INITIATED_EVENT);
	}

	@Test
	void handleIncidentRequestExisting() {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var incidentRequest = IncidentRequest.create()
			.withIncidentKey(pobIssueKey);
		final var existingEntity = IncidentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withStatus(SYNCHRONIZED);

		when(incidentRepositoryMock.findByPobIssueKey(pobIssueKey)).thenReturn(Optional.of(existingEntity));

		// Act
		incidentService.handleIncidentRequest(incidentRequest);

		// Assert
		verify(incidentRepositoryMock).findByPobIssueKey(pobIssueKey);
		verify(incidentRepositoryMock).save(incidentEntityCaptor.capture());

		final var capturedValue = incidentEntityCaptor.getValue();
		assertThat(capturedValue.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedValue.getPobIssueLastModified()).isCloseTo(now(), within(2, SECONDS));
		assertThat(capturedValue.getStatus()).isEqualTo(POB_INITIATED_EVENT);
	}

	@Test
	void handleIncidentRequestStatusIsNotModifiable() {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var incidentRequest = IncidentRequest.create()
			.withIncidentKey(pobIssueKey);
		final var existingEntity = IncidentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withStatus(JIRA_INITIATED_EVENT);

		when(incidentRepositoryMock.findByPobIssueKey(pobIssueKey)).thenReturn(Optional.of(existingEntity));

		// Act
		incidentService.handleIncidentRequest(incidentRequest);

		// Assert
		verify(incidentRepositoryMock).findByPobIssueKey(pobIssueKey);
		verify(incidentRepositoryMock).save(incidentEntityCaptor.capture());

		final var capturedValue = incidentEntityCaptor.getValue();
		assertThat(capturedValue.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedValue.getPobIssueLastModified()).isCloseTo(now(), within(2, SECONDS));
		assertThat(capturedValue.getStatus()).isEqualTo(JIRA_INITIATED_EVENT);
	}
}
