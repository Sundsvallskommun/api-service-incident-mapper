package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.CLOSED;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;
import static se.sundsvall.incidentmapper.service.IncidentService.PROBLEM;
import static se.sundsvall.incidentmapper.service.IncidentService.SCOPE_ALL;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.joda.time.DateTime;
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
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;
import se.sundsvall.incidentmapper.integration.jira.JiraClient;
import se.sundsvall.incidentmapper.integration.pob.POBClient;

import generated.se.sundsvall.pob.PobMemo;
import generated.se.sundsvall.pob.PobPayload;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

	@Mock
	private IncidentRepository incidentRepositoryMock;

	@Mock
	private JiraClient jiraClientMock;

	@Mock
	private Issue jiraIssueMock;

	@Mock
	private POBClient pobClientMock;

	@Mock
	private Attachment attachmentMock;

	@Mock
	private Comment commentMock;

	@Mock
	private com.atlassian.jira.rest.client.api.domain.Status statusMock;

	@InjectMocks
	private IncidentService incidentService;

	@Captor
	private ArgumentCaptor<IncidentEntity> incidentEntityCaptor;

	@Captor
	private ArgumentCaptor<OffsetDateTime> offsetDateTimeCaptor;

	@Captor
	private ArgumentCaptor<Status> statusCaptor;

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
		assertThat(capturedValue.getStatus()).isEqualTo(JIRA_INITIATED_EVENT);
	}

	@Test
	void pollJiraUpdatesWhenUpdatesFound() {

		// Arrange
		final var jiraIssueKey = "JIR-12345";
		final var lastSynchronizedJira = now().minusDays(1);
		final var existingIncident = IncidentEntity.create()
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedJira(lastSynchronizedJira)
			.withStatus(SYNCHRONIZED);

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(List.of(existingIncident));
		when(jiraIssueMock.getUpdateDate()).thenReturn(DateTime.now());
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssueMock));

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(jiraIssueMock).getUpdateDate();
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(JIRA_INITIATED_EVENT);
	}

	@Test
	void pollJiraUpdatesWhenUpdatesNotFound() {

		// Arrange
		final var jiraIssueKey = "JIR-12345";
		final var lastSynchronizedJira = now().plusMinutes(1);
		final var existingIncident = IncidentEntity.create()
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedJira(lastSynchronizedJira)
			.withStatus(SYNCHRONIZED);

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(List.of(existingIncident));
		when(jiraIssueMock.getUpdateDate()).thenReturn(DateTime.now());
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssueMock));

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(jiraIssueMock).getUpdateDate();
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void pollJiraUpdatesWhenJiraIssueNotFound() {

		// Arrange
		final var jiraIssueKey = "JIR-12345";
		final var lastSynchronizedJira = now().plusMinutes(1);
		final var existingIncident = IncidentEntity.create()
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedJira(lastSynchronizedJira)
			.withStatus(SYNCHRONIZED);

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(List.of(existingIncident));
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.empty());

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verifyNoInteractions(jiraIssueMock);
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void pollJiraUpdatesWhenIncidentMappingsNotFound() {

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(emptyList());

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verifyNoInteractions(jiraClientMock, jiraIssueMock);
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void cleanObsoleteIncidents() {

		// Act
		incidentService.cleanObsoleteIncidents();

		// Assert
		verify(incidentRepositoryMock).deleteByModifiedBeforeAndStatusIn(offsetDateTimeCaptor.capture(), statusCaptor.capture());

		final var capturedOffsetDateTime = offsetDateTimeCaptor.getValue();
		final var capturedStatus = statusCaptor.getAllValues();

		assertThat(capturedOffsetDateTime).isCloseTo(now(systemDefault()).minusDays(10), within(2, SECONDS));
		assertThat(capturedStatus).isEqualTo(List.of(CLOSED));
	}

	@Test
	void updatePob() {
		// Arrange
		final var incidentEntity = IncidentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withJiraIssueKey("JIR-12345")
			.withPobIssueKey("POB-12345")
			.withLastSynchronizedPob(now().minusDays(1))
			.withStatus(JIRA_INITIATED_EVENT);

		final var pobAttachments = new PobPayload();
		final var jiraAttachments = List.of(attachmentMock);
		final var memoPayload = new PobPayload().memo(Map.of(PROBLEM, new PobMemo()));

		when(incidentRepositoryMock.findByStatus(JIRA_INITIATED_EVENT)).thenReturn(List.of(incidentEntity));
		when(jiraClientMock.getIssue(incidentEntity.getJiraIssueKey())).thenReturn(Optional.of(jiraIssueMock));
		when(jiraClientMock.getAttachment(attachmentMock.getContentUri())).thenReturn(new java.io.ByteArrayInputStream("test".getBytes()));

		when(jiraIssueMock.getAttachments()).thenReturn(jiraAttachments);
		when(jiraIssueMock.getStatus()).thenReturn(statusMock);
		when(statusMock.getName()).thenReturn("Done");

		when(jiraIssueMock.getDescription()).thenReturn("SomeDescription");
		when(jiraIssueMock.getComments()).thenReturn(List.of(commentMock));
		when(commentMock.getCreationDate()).thenReturn(DateTime.now());
		when(commentMock.getAuthor()).thenReturn(new BasicUser(URI.create("self"), "notSystem", "notSystem"));
		when(commentMock.getBody()).thenReturn("SomeComment");

		when(pobClientMock.getAttachments(incidentEntity.getPobIssueKey())).thenReturn(pobAttachments);
		when(pobClientMock.getMemo(incidentEntity.getPobIssueKey(), PROBLEM, SCOPE_ALL)).thenReturn(memoPayload);

		// Act
		incidentService.updatePob();

		// Assert
		verify(incidentRepositoryMock).findByStatus(JIRA_INITIATED_EVENT);
		verify(jiraClientMock).getIssue(incidentEntity.getJiraIssueKey());
		verify(pobClientMock).getAttachments(incidentEntity.getPobIssueKey());
		verify(pobClientMock).getMemo(incidentEntity.getPobIssueKey(), PROBLEM, SCOPE_ALL);
		verify(pobClientMock, times(3)).updateCase(any());
		verify(pobClientMock).createAttachment(any(), any());
		verify(incidentRepositoryMock).saveAndFlush(incidentEntity);

	}

}
