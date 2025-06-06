package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.now;
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
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;
import static se.sundsvall.incidentmapper.service.IncidentService.JIRA_ISSUE_CREATED;

import com.chavaillaz.client.jira.domain.Attachment;
import com.chavaillaz.client.jira.domain.Attachments;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Comments;
import com.chavaillaz.client.jira.domain.Fields;
import com.chavaillaz.client.jira.domain.Issue;
import com.chavaillaz.client.jira.domain.Transition;
import com.chavaillaz.client.jira.domain.User;
import generated.se.sundsvall.pob.PobMemo;
import generated.se.sundsvall.pob.PobPayload;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.jira.JiraIncidentClient;
import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;
import se.sundsvall.incidentmapper.integration.pob.POBClient;
import se.sundsvall.incidentmapper.service.configuration.SynchronizationProperties;
import se.sundsvall.incidentmapper.service.mapper.PobMapper;

@ExtendWith({
	MockitoExtension.class, ResourceLoaderExtension.class
})
class IncidentServiceTest {

	private static final String TEMP_DIR = "target/tmp";

	@Mock
	private IncidentRepository incidentRepositoryMock;

	@Mock
	private JiraIncidentClient jiraClientMock;

	@Mock
	private POBClient pobClientMock;

	@Mock
	private SlackService slackServiceMock;

	@Mock
	private SynchronizationProperties synchronizationPropertiesMock;

	@Mock
	private InputStreamResource inputStreamResourceMock;

	@Mock
	private ResponseEntity<InputStreamResource> responseEntityMock;

	@InjectMocks
	private IncidentService incidentService;

	@Captor
	private ArgumentCaptor<IncidentEntity> incidentEntityCaptor;

	@Captor
	private ArgumentCaptor<OffsetDateTime> offsetDateTimeCaptor;

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
		final var municipalityId = "2281";
		final var pobIssueKey = "POB-12345";
		final var incidentRequest = IncidentRequest.create()
			.withIncidentKey(pobIssueKey);

		when(incidentRepositoryMock.findByMunicipalityIdAndPobIssueKey(municipalityId, pobIssueKey)).thenReturn(empty());

		// Act
		incidentService.handleIncidentRequest(municipalityId, incidentRequest);

		// Assert
		verify(incidentRepositoryMock).findByMunicipalityIdAndPobIssueKey(municipalityId, pobIssueKey);
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

		final var capturedValue = incidentEntityCaptor.getValue();
		assertThat(capturedValue.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedValue.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(capturedValue.getStatus()).isEqualTo(POB_INITIATED_EVENT);
	}

	@Test
	void handleIncidentRequestExisting() {

		// Arrange
		final var municipalityId = "2281";
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var incidentRequest = IncidentRequest.create()
			.withIncidentKey(pobIssueKey);
		final var existingEntity = IncidentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withStatus(SYNCHRONIZED);

		when(incidentRepositoryMock.findByMunicipalityIdAndPobIssueKey(municipalityId, pobIssueKey)).thenReturn(Optional.of(existingEntity));

		// Act
		incidentService.handleIncidentRequest(municipalityId, incidentRequest);

		// Assert
		verify(incidentRepositoryMock).findByMunicipalityIdAndPobIssueKey(municipalityId, pobIssueKey);
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

		final var capturedValue = incidentEntityCaptor.getValue();
		assertThat(capturedValue.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedValue.getStatus()).isEqualTo(POB_INITIATED_EVENT);
	}

	@Test
	void handleIncidentRequestStatusIsNotModifiable() {

		// Arrange
		final var municipalityId = "2281";
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var incidentRequest = IncidentRequest.create()
			.withIncidentKey(pobIssueKey);
		final var existingEntity = IncidentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withStatus(JIRA_INITIATED_EVENT);

		when(incidentRepositoryMock.findByMunicipalityIdAndPobIssueKey(municipalityId, pobIssueKey)).thenReturn(Optional.of(existingEntity));

		// Act
		incidentService.handleIncidentRequest(municipalityId, incidentRequest);

		// Assert
		verify(incidentRepositoryMock).findByMunicipalityIdAndPobIssueKey(municipalityId, pobIssueKey);
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(JIRA_INITIATED_EVENT);
	}

	@Test
	void pollJiraIssuesWhenUpdatesFound() {

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
		incidentService.pollJiraIssues();

		// Assert
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(JIRA_INITIATED_EVENT);
	}

	@Test
	void pollJiraIssuesWhenUpdatesNotFound() {

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
		incidentService.pollJiraIssues();

		// Assert
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void pollJiraIssuesWhenJiraIssueNotFound() {

		// Arrange
		final var jiraIssueKey = "JIR-12345";
		final var pobIssueKey = "POB-12345";
		final var lastSynchronizedJira = now().plusMinutes(1);
		final var existingIncident = IncidentEntity.create()
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withLastSynchronizedJira(lastSynchronizedJira)
			.withStatus(SYNCHRONIZED);

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(List.of(existingIncident));
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.empty());

		// Act
		incidentService.pollJiraIssues();

		// Assert
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(POB_INITIATED_EVENT);
		assertThat(capturedIncidentEntity.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedIncidentEntity.getJiraIssueKey()).isNull();
		assertThat(capturedIncidentEntity.getLastSynchronizedJira()).isNull();
	}

	@Test
	void pollJiraIssuesWhenIncidentMappingsNotFound() {

		when(incidentRepositoryMock.findByStatus(SYNCHRONIZED)).thenReturn(emptyList());

		// Act
		incidentService.pollJiraIssues();

		// Assert
		verifyNoInteractions(jiraClientMock);
		verify(incidentRepositoryMock).findByStatus(SYNCHRONIZED);
		verify(incidentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updatePobIssues() {

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
		attachment.setContent("contentUrl");
		attachment.setFilename(file.getName());

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
		when(incidentRepositoryMock.findByStatus(JIRA_INITIATED_EVENT)).thenReturn(List.of(incidentEntity));
		when(jiraClientMock.getIssue(incidentEntity.getJiraIssueKey())).thenReturn(Optional.of(jiraIssue));
		when(jiraClientMock.getProperties()).thenReturn(new JiraProperties("user", null, null, null));
		when(pobClientMock.getAttachments(incidentEntity.getPobIssueKey())).thenReturn(Optional.of(pobAttachments));
		when(pobClientMock.getProblemMemo(incidentEntity.getPobIssueKey())).thenReturn(Optional.of(memoPayload));

		// Act
		incidentService.updatePobIssues();

		// Assert
		verify(incidentRepositoryMock).findByStatus(JIRA_INITIATED_EVENT);
		verify(jiraClientMock, times(2)).getIssue(incidentEntity.getJiraIssueKey());
		verify(pobClientMock, times(2)).getAttachments(incidentEntity.getPobIssueKey());
		verify(pobClientMock, times(2)).getProblemMemo(incidentEntity.getPobIssueKey());
		verify(pobClientMock, times(2)).updateCase(any());
		verify(pobClientMock).createAttachment(any(), any());
		verify(incidentRepositoryMock, times(2)).saveAndFlush(incidentEntity);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"Closed", "Done", "Review Done", "Resolved", "Won't Do"
	})
	void closeIssues(String statusName) {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var pobFirstLineUserGroup = "The-user-group";
		final var incidentEntity = IncidentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withLastSynchronizedPob(now().minusDays(1))
			.withStatus(SYNCHRONIZED);

		final var pobPayload = PobMapper.toResponsibleGroupPayload(pobIssueKey, pobFirstLineUserGroup);

		final var status = new com.chavaillaz.client.jira.domain.Status();
		status.setName(statusName);

		final var fields = new Fields();
		fields.setStatus(status);

		final var jiraIssue = new Issue();
		jiraIssue.setFields(fields);

		when(synchronizationPropertiesMock.responsibleUserGroupInPob()).thenReturn(pobFirstLineUserGroup);
		when(incidentRepositoryMock.findAll()).thenReturn(List.of(incidentEntity));
		when(jiraClientMock.getIssue(incidentEntity.getJiraIssueKey())).thenReturn(Optional.of(jiraIssue));

		// Act
		incidentService.closeIssues();

		// Assert
		verify(incidentRepositoryMock).findAll();
		verify(jiraClientMock).getIssue(incidentEntity.getJiraIssueKey());
		verify(pobClientMock).updateCase(pobPayload);
		verify(incidentRepositoryMock).delete(incidentEntity);
	}

	@Test
	void updateJiraIssue(
		@Load(value = "/IncidentServiceTest/pobPayloadCase.json", as = JSON) final PobPayload pobPayload,
		@Load(value = "/IncidentServiceTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) final PobPayload pobPayloadCaseInternalNotesCustomMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadProblemMemo.json", as = JSON) final PobPayload pobPayloadProblemMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadAttachments.json", as = JSON) final PobPayload pobPayloadAttachments,
		@Load(value = "/IncidentServiceTest/pobPayloadMail.json", as = JSON) final PobPayload pobPayloadMail,
		@Load(value = "/IncidentServiceTest/pobPayloadReceivedMailIds.json", as = JSON) final PobPayload pobPayloadReceivedMailId,
		@Load(value = "/IncidentServiceTest/pobPayloadMailAttachments.json", as = JSON) final PobPayload pobPayloadMailAttachments) throws Exception {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var jiraIssue = Issue.fromKey(jiraIssueKey);
		final var attachmentId = "attachmentId";
		final var attachment = new Attachment();
		final var mailId = "mailId";
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
		when(pobClientMock.getAttachment(pobIssueKey, "1628120")).thenReturn(responseEntityMock);
		when(responseEntityMock.getBody()).thenReturn(inputStreamResourceMock);
		when(responseEntityMock.getHeaders()).thenReturn(new HttpHeaders());
		when(pobClientMock.getReceivedMailIds(pobIssueKey)).thenReturn(List.of(pobPayloadReceivedMailId));
		when(pobClientMock.getMail(mailId)).thenReturn(Optional.of(pobPayloadMail));
		when(inputStreamResourceMock.getInputStream()).thenReturn(new FileInputStream(file));
		when(incidentRepositoryMock.findByStatus(POB_INITIATED_EVENT)).thenReturn(List.of(
			IncidentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withPobIssueKey(pobIssueKey)
				.withJiraIssueKey(jiraIssueKey)
				.withStatus(POB_INITIATED_EVENT)));

		// Act
		incidentService.updateJiraIssues();

		// Assert
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());
		verify(jiraClientMock, never()).createIssue(any(), any(), any(), any());
		verify(jiraClientMock).updateIssue(jiraIssueCaptor.capture());
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(jiraClientMock).addComment(jiraIssueKey, "2024-05-08 14:09 Kommentar");
		verify(jiraClientMock).deleteAttachment(attachmentId);
		verify(jiraClientMock).addAttachment(jiraIssueKey, new File(TEMP_DIR + "/" + pobIssueKey + "/jatteglad_hund.png"));
		verify(pobClientMock).getCase(pobIssueKey);
		verify(pobClientMock).getCaseInternalNotesCustom(pobIssueKey);
		verify(pobClientMock).getProblemMemo(pobIssueKey);
		verify(pobClientMock).getAttachments(pobIssueKey);
		verify(pobClientMock).getAttachment(pobIssueKey, "1628120");
		verify(pobClientMock).getReceivedMailIds(pobIssueKey);
		verify(pobClientMock).getMail(mailId);

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
		assertThat(capturedJiraIssuey.getFields().getSummary()).isEqualTo("Supportärende POB-12345 (This works!)");
	}

	@Test
	void updateJiraIssueWhenIssueNotFoundInJira(
		@Load(value = "/IncidentServiceTest/pobPayloadCase.json", as = JSON) final PobPayload pobPayload,
		@Load(value = "/IncidentServiceTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) final PobPayload pobPayloadCaseInternalNotesCustomMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadProblemMemo.json", as = JSON) final PobPayload pobPayloadProblemMemo) {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";

		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(empty());
		when(pobClientMock.getCase(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayload));
		when(pobClientMock.getCaseInternalNotesCustom(pobIssueKey)).thenReturn(Optional.of(pobPayloadCaseInternalNotesCustomMemo));
		when(pobClientMock.getProblemMemo(pobIssueKey)).thenReturn(Optional.of(pobPayloadProblemMemo));
		when(incidentRepositoryMock.findByStatus(POB_INITIATED_EVENT)).thenReturn(List.of(
			IncidentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withPobIssueKey(pobIssueKey)
				.withJiraIssueKey(jiraIssueKey)
				.withStatus(POB_INITIATED_EVENT)));

		// Act
		incidentService.updateJiraIssues();

		// Assert
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());
		verify(jiraClientMock, never()).createIssue(any(), any(), any(), any());
		verify(jiraClientMock).getIssue(jiraIssueKey);

		verify(pobClientMock).getCase(pobIssueKey);
		verify(pobClientMock).getCaseInternalNotesCustom(pobIssueKey);
		verify(pobClientMock).getProblemMemo(pobIssueKey);

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(POB_INITIATED_EVENT);
		assertThat(capturedIncidentEntity.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedIncidentEntity.getJiraIssueKey()).isNull();
		assertThat(capturedIncidentEntity.getLastSynchronizedJira()).isNull();
	}

	@Test
	void createJiraIssue(
		@Load(value = "/IncidentServiceTest/pobPayloadCase.json", as = JSON) final PobPayload pobPayload,
		@Load(value = "/IncidentServiceTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) final PobPayload pobPayloadCaseInternalNotesCustomMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadProblemMemo.json", as = JSON) final PobPayload pobPayloadProblemMemo,
		@Load(value = "/IncidentServiceTest/pobPayloadAttachments.json", as = JSON) final PobPayload pobPayloadAttachments,
		@Load(value = "/IncidentServiceTest/pobPayloadMail.json", as = JSON) final PobPayload pobPayloadMail,
		@Load(value = "/IncidentServiceTest/pobPayloadReceivedMailIds.json", as = JSON) final PobPayload pobPayloadReceivedMailId,
		@Load(value = "/IncidentServiceTest/pobPayloadMailAttachments.json", as = JSON) final PobPayload pobPayloadMailAttachments) throws Exception {

		// Arrange
		final var pobIssueKey = "POB-12345";
		final var jiraIssueKey = "JIR-12345";
		final var jiraIssue = new Issue();
		final var initialTransition = Transition.fromName("To Do");
		final var mailId = "mailId";
		final var municipalityId = "2281";

		when(synchronizationPropertiesMock.tempFolder()).thenReturn(TEMP_DIR);
		when(jiraClientMock.createIssue(any(), any(), any(), any())).thenReturn(jiraIssueKey);
		when(jiraClientMock.getIssue(jiraIssueKey)).thenReturn(Optional.of(jiraIssue));
		when(jiraClientMock.getProperties()).thenReturn(new JiraProperties("user", "pass", "http:://jira-test.com", "XX"));
		when(jiraClientMock.getTransitions(jiraIssueKey)).thenReturn(Map.of(initialTransition.getName(), initialTransition));
		when(pobClientMock.getCase(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayload));
		when(pobClientMock.getCaseInternalNotesCustom(pobIssueKey)).thenReturn(Optional.of(pobPayloadCaseInternalNotesCustomMemo));
		when(pobClientMock.getProblemMemo(pobIssueKey)).thenReturn(Optional.of(pobPayloadProblemMemo));
		when(pobClientMock.getAttachments(pobIssueKey)).thenReturn(Optional.ofNullable(pobPayloadAttachments));
		when(pobClientMock.getAttachment(pobIssueKey, "1628120")).thenReturn(responseEntityMock);
		when(responseEntityMock.getBody()).thenReturn(inputStreamResourceMock);
		when(responseEntityMock.getHeaders()).thenReturn(new HttpHeaders());
		when(pobClientMock.getReceivedMailIds(pobIssueKey)).thenReturn(List.of(pobPayloadReceivedMailId));
		when(pobClientMock.getMail(mailId)).thenReturn(Optional.of(pobPayloadMail));
		when(inputStreamResourceMock.getInputStream()).thenReturn(new FileInputStream(file));
		when(incidentRepositoryMock.findByStatus(POB_INITIATED_EVENT)).thenReturn(List.of(
			IncidentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withPobIssueKey(pobIssueKey)
				.withMunicipalityId(municipalityId)
				.withStatus(POB_INITIATED_EVENT)));

		// Act
		incidentService.updateJiraIssues();

		// Assert
		verify(incidentRepositoryMock).saveAndFlush(incidentEntityCaptor.capture());
		verify(jiraClientMock).createIssue("Bug", List.of("support-ticket"), "Supportärende POB-12345 (This works!)", "This is a description");
		verify(jiraClientMock).getTransitions(jiraIssueKey);
		verify(jiraClientMock).performTransition(jiraIssueKey, initialTransition);
		verify(jiraClientMock).getIssue(jiraIssueKey);
		verify(jiraClientMock).addComment(jiraIssueKey, "2024-05-08 14:09 Kommentar");
		verify(jiraClientMock).addAttachment(jiraIssueKey, new File(TEMP_DIR + "/" + pobIssueKey + "/jatteglad_hund.png"));
		verify(pobClientMock).getCase(pobIssueKey);
		verify(pobClientMock).getCaseInternalNotesCustom(pobIssueKey);
		verify(pobClientMock).getProblemMemo(pobIssueKey);
		verify(pobClientMock).getAttachments(pobIssueKey);
		verify(pobClientMock).getAttachment(pobIssueKey, "1628120");
		verify(pobClientMock).getReceivedMailIds(pobIssueKey);
		verify(pobClientMock).getMail(mailId);
		verify(slackServiceMock).sendToSlack(municipalityId, JIRA_ISSUE_CREATED.formatted("This works!", "http:://jira-test.com", "JIR-12345"));

		final var capturedIncidentEntity = incidentEntityCaptor.getValue();
		assertThat(capturedIncidentEntity).isNotNull();
		assertThat(capturedIncidentEntity.getStatus()).isEqualTo(SYNCHRONIZED);
		assertThat(capturedIncidentEntity.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(capturedIncidentEntity.getJiraIssueKey()).isEqualTo(jiraIssueKey);
		assertThat(capturedIncidentEntity.getLastSynchronizedJira()).isCloseTo(now(), within(2, SECONDS));
	}
}
