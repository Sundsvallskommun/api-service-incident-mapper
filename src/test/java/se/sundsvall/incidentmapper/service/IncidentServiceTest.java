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
import static se.sundsvall.dept44.test.annotation.resource.Load.ResourceType.JSON;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.CLOSED;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;

import java.io.File;
import java.io.FileInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;

import com.chavaillaz.client.jira.domain.Attachment;
import com.chavaillaz.client.jira.domain.Attachments;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Comments;
import com.chavaillaz.client.jira.domain.Fields;
import com.chavaillaz.client.jira.domain.Issue;
import com.chavaillaz.client.jira.domain.User;

import generated.se.sundsvall.pob.PobMemo;
import generated.se.sundsvall.pob.PobPayload;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;
import se.sundsvall.incidentmapper.integration.jira.JiraIncidentClient;
import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;
import se.sundsvall.incidentmapper.integration.pob.POBClient;
import se.sundsvall.incidentmapper.service.configuration.SynchronizationProperties;

@ExtendWith({ MockitoExtension.class, ResourceLoaderExtension.class })
class IncidentServiceTest {

	private static final String TEMP_DIR = "target/tmp";

	@Mock
	private IncidentRepository incidentRepositoryMock;

	@Mock
	private JiraIncidentClient jiraClientMock;

	@Mock
	private POBClient pobClientMock;

	@Mock
	private SynchronizationProperties synchronizationPropertiesMock;

	@Mock
	private InputStreamResource inputStreamResourceMock;

	@InjectMocks
	private IncidentService incidentService;

	@Captor
	private ArgumentCaptor<IncidentEntity> incidentEntityCaptor;

	@Captor
	private ArgumentCaptor<OffsetDateTime> offsetDateTimeCaptor;

	@Captor
	private ArgumentCaptor<Status> statusCaptor;

	@Captor
	private ArgumentCaptor<Issue> jiraIssueCaptor;

	private File file;

	@BeforeEach
	void before() throws Exception {

		file = new File(TEMP_DIR + "/test.png");
		file.getParentFile().mkdirs();
		file.createNewFile();
	}

	@AfterEach
	void after() {
		file.delete();
	}

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
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

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
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

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
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void pollJiraUpdatesWhenUpdatesFound() {

		// Arrange
		final var jiraIssueKey = "JIR-12345";
		final var lastSynchronizedPob = now().minusDays(1);
		final var existingIncident = IncidentEntity.create()
			.withJiraIssueKey(jiraIssueKey)
			.withLastSynchronizedPob(lastSynchronizedPob)
			.withStatus(SYNCHRONIZED);

		final var jiraIssue = Issue.fromKey(jiraIssueKey);
		jiraIssue.getFields().setUpdated(now());

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(List.of(existingIncident));
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssue));

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verify(jiraClientMock).getIssue(jiraIssueKey);
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

		final var fields = new Fields();
		fields.setUpdated(now());
		final var jiraIssue = new Issue();
		jiraIssue.setFields(fields);

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(List.of(existingIncident));
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssue));

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verify(jiraClientMock).getIssue(jiraIssueKey);
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
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void pollJiraUpdatesWhenIncidentMappingsNotFound() {

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(emptyList());

		// Act
		incidentService.pollJiraUpdates();

		// Assert
		verifyNoInteractions(jiraClientMock);
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
		final var memoPayload = new PobPayload().memo(Map.of("Problem", new PobMemo()));

		final var attachment = new Attachment();
		attachment.setContent("testContent");
		final var attachments = new Attachments();
		attachments.add(attachment);

		final var user = new User();
		user.setName("someUser");

		final var comments = new Comments();
		final var comment = new Comment();
		comment.setBody("testComment");
		comment.setCreated(now());
		comment.setAuthor(user);
		comments.add(comment);

		final var status = new com.chavaillaz.client.jira.domain.Status();
		status.setName("Done");

		final var fields = new Fields();
		fields.setAttachments(attachments);
		fields.setStatus(status);
		fields.setDescription("SomeDescription");
		fields.setComments(comments);

		final var jiraIssue = new Issue();
		jiraIssue.setFields(fields);

		when(synchronizationPropertiesMock.tempFolder()).thenReturn(TEMP_DIR);
		when(synchronizationPropertiesMock.responsibleUserGroupInPob()).thenReturn("The-user-group");
		when(incidentRepositoryMock.findByStatus(JIRA_INITIATED_EVENT)).thenReturn(List.of(incidentEntity));
		when(jiraClientMock.getIssue(incidentEntity.getJiraIssueKey())).thenReturn(Optional.of(jiraIssue));
		when(jiraClientMock.getProperties()).thenReturn(new JiraProperties("user", null, null, null));
		when(pobClientMock.getAttachments(incidentEntity.getPobIssueKey())).thenReturn(Optional.of(pobAttachments));
		when(pobClientMock.getProblemMemo(incidentEntity.getPobIssueKey())).thenReturn(Optional.of(memoPayload));

		// Act
		incidentService.updatePob();

		// Assert
		verify(incidentRepositoryMock).findByStatus(JIRA_INITIATED_EVENT);
		verify(jiraClientMock, times(2)).getIssue(incidentEntity.getJiraIssueKey());
		verify(pobClientMock, times(2)).getAttachments(incidentEntity.getPobIssueKey());
		verify(pobClientMock, times(2)).getProblemMemo(incidentEntity.getPobIssueKey());
		verify(pobClientMock, times(3)).updateCase(any());
		verify(pobClientMock).createAttachment(any(), any());
		verify(incidentRepositoryMock, times(2)).saveAndFlush(incidentEntity);
	}

	@Test
	void updateJiraIssue(
		@Load(value = "/IncidentServiceTest/pobPayloadCase.json", as = JSON) final PobPayload pobPayload,
		@Load(value = "/IncidentServiceTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) final PobPayload pobPayloadCaseInternalNotesCustomMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadProblemMemo.json", as = JSON) final PobPayload pobPayloadProblemMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadAttachments.json", as = JSON) final PobPayload pobPayloadAttachments) throws Exception {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var jiraIssue = Issue.fromKey(jiraIssueKey);
		final var attachmentId = "attachmentId";
		final var attachment = new Attachment();
		attachment.setId(attachmentId);
		attachment.setFilename("test.jpg");
		jiraIssue.getFields().setComments(new Comments());
		jiraIssue.getFields().setAttachments(Attachments.from(attachment));
		jiraIssue.getFields().setStatus(com.chavaillaz.client.jira.domain.Status.fromName("Closed"));

		when(synchronizationPropertiesMock.tempFolder()).thenReturn(TEMP_DIR);
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssue));
		when(pobClientMock.getCase(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayload));
		when(pobClientMock.getCaseInternalNotesCustom(pobIssueKey)).thenReturn(Optional.of(pobPayloadCaseInternalNotesCustomMemo));
		when(pobClientMock.getProblemMemo(pobIssueKey)).thenReturn(Optional.of(pobPayloadProblemMemo));
		when(pobClientMock.getAttachments(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayloadAttachments));
		when(pobClientMock.getAttachment(pobIssueKey, "1628120")).thenReturn(inputStreamResourceMock);
		when(inputStreamResourceMock.getInputStream()).thenReturn(new FileInputStream(file));
		when(incidentRepositoryMock.findByStatus(POB_INITIATED_EVENT)).thenReturn(List.of(
			IncidentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withPobIssueKey(pobIssueKey)
				.withJiraIssueKey(jiraIssueKey)
				.withStatus(POB_INITIATED_EVENT)));

		// Act
		incidentService.updateJira();

		// Assert
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());
		verify(jiraClientMock, never()).createIssue(any(), any(), any());
		verify(jiraClientMock).updateIssue(jiraIssueCaptor.capture());
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(jiraClientMock).addComment(jiraIssueKey, "2024-05-08 14:09 Kommentar");
		verify(jiraClientMock).deleteAttachment(attachmentId);
		verify(jiraClientMock).addAttachment(jiraIssueKey, new File(TEMP_DIR + "/happy_dog.png"));
		verify(pobClientMock).getCase(pobIssueKey);
		verify(pobClientMock).getCaseInternalNotesCustom(pobIssueKey);
		verify(pobClientMock).getProblemMemo(pobIssueKey);
		verify(pobClientMock).getAttachments(pobIssueKey);
		verify(pobClientMock).getAttachment(pobIssueKey, "1628120");
		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(SYNCHRONIZED);
		assertThat(capturedIncidentEntity.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedIncidentEntity.getJiraIssueKey()).isEqualTo(jiraIssueKey);
		assertThat(capturedIncidentEntity.getLastSynchronizedJira()).isCloseTo(now(), within(2, SECONDS));

		final var capturedJiraIssuey = jiraIssueCaptor.getValue();
		assertThat(capturedJiraIssuey).isNotNull();
		assertThat(capturedJiraIssuey.getFields()).hasAllNullFieldsOrPropertiesExcept("description", "summary", "status", "customFields");
		assertThat(capturedJiraIssuey.getFields().getDescription()).isEqualTo("This is a description");
		assertThat(capturedJiraIssuey.getFields().getSummary()).isEqualTo("This works!");
		assertThat(capturedJiraIssuey.getFields().getStatus().getName()).isEqualTo("To Do");
	}

	@Test
	void createJiraIssue(
		@Load(value = "/IncidentServiceTest/pobPayloadCase.json", as = JSON) final PobPayload pobPayload,
		@Load(value = "/IncidentServiceTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) final PobPayload pobPayloadCaseInternalNotesCustomMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadProblemMemo.json", as = JSON) final PobPayload pobPayloadProblemMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadAttachments.json", as = JSON) final PobPayload pobPayloadAttachments) throws Exception {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var jiraIssue = new Issue();

		when(synchronizationPropertiesMock.tempFolder()).thenReturn(TEMP_DIR);
		when(jiraClientMock.createIssue(any(), any(), any())).thenReturn(jiraIssueKey);
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssue));
		when(pobClientMock.getCase(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayload));
		when(pobClientMock.getCaseInternalNotesCustom(pobIssueKey)).thenReturn(Optional.of(pobPayloadCaseInternalNotesCustomMemo));
		when(pobClientMock.getProblemMemo(pobIssueKey)).thenReturn(Optional.of(pobPayloadProblemMemo));
		when(pobClientMock.getAttachments(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayloadAttachments));
		when(pobClientMock.getAttachment(pobIssueKey, "1628120")).thenReturn(inputStreamResourceMock);
		when(inputStreamResourceMock.getInputStream()).thenReturn(new FileInputStream(file));
		when(incidentRepositoryMock.findByStatus(POB_INITIATED_EVENT)).thenReturn(List.of(
			IncidentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withPobIssueKey(pobIssueKey)
				.withStatus(POB_INITIATED_EVENT)));

		// Act
		incidentService.updateJira();

		// Assert
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());
		verify(jiraClientMock).createIssue("Bug", "Supportärende (This works!).", "This is a description");
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(jiraClientMock).addComment(jiraIssueKey, "2024-05-08 14:09 Kommentar");
		verify(jiraClientMock).addAttachment(jiraIssueKey, new File(TEMP_DIR + "/happy_dog.png"));
		verify(pobClientMock).getCase(pobIssueKey);
		verify(pobClientMock).getCaseInternalNotesCustom(pobIssueKey);
		verify(pobClientMock).getProblemMemo(pobIssueKey);
		verify(pobClientMock).getAttachments(pobIssueKey);
		verify(pobClientMock).getAttachment(pobIssueKey, "1628120");

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(SYNCHRONIZED);
		assertThat(capturedIncidentEntity.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedIncidentEntity.getJiraIssueKey()).isEqualTo(jiraIssueKey);
		assertThat(capturedIncidentEntity.getLastSynchronizedJira()).isCloseTo(now(), within(2, SECONDS));
	}
}
