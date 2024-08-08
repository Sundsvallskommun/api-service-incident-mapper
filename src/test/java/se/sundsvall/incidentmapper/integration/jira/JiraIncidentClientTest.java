package se.sundsvall.incidentmapper.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavaillaz.client.jira.JiraClient;
import com.chavaillaz.client.jira.api.IssueApi;
import com.chavaillaz.client.jira.api.ProjectApi;
import com.chavaillaz.client.jira.domain.Attachment;
import com.chavaillaz.client.jira.domain.Attachments;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Fields;
import com.chavaillaz.client.jira.domain.Identity;
import com.chavaillaz.client.jira.domain.Issue;
import com.chavaillaz.client.jira.domain.IssueType;
import com.chavaillaz.client.jira.domain.Project;
import com.chavaillaz.client.jira.domain.Transitions;

import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;

@ExtendWith(MockitoExtension.class)
class JiraIncidentClientTest {

	@Mock
	private Issue issueMock;

	@Mock
	private IssueApi<Issue> issueApiMock;

	@Mock
	private ProjectApi projectApiMock;

	@Mock
	private JiraClient<Issue> jiraClientMock;

	@Mock
	private JiraProperties jiraPropertiesMock;

	@Mock
	private CompletableFuture<Transitions> completableFutureTransitionsMock;

	@Mock
	private CompletableFuture<Identity> completableFutureIdentityMock;

	@Mock
	private CompletableFuture<Issue> completableFutureIssueMock;

	@Mock
	private CompletableFuture<Attachment> completableFutureAttachmentMock;

	@Mock
	private CompletableFuture<Attachments> completableFutureAttachmentsMock;

	@Mock
	private CompletableFuture<InputStream> completableFutureInputStreamMock;

	@InjectMocks
	private JiraIncidentClient jiraClient;

	private File file;

	@BeforeEach
	void before() throws Exception {
		file = new File("tmp");
		file.createNewFile();
	}

	@AfterEach
	void after() {
		file.delete();
	}

	@Test
	void getProperties() {

		// Act
		final var result = jiraClient.getProperties();

		// Assert
		assertThat(result).isEqualTo(jiraPropertiesMock);
	}

	@Test
	void createIssue() throws Exception {

		// Arrange
		final var projectKey = "TEST";
		final var type = "Bug";
		final var summary = "Test issue";
		final var description = "Test description";
		final var key = "TEST-1";
		final var labels = List.of("my-label");

		when(issueMock.getKey()).thenReturn(key);
		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.addIssue(any())).thenReturn(completableFutureIdentityMock);
		when(completableFutureIdentityMock.get()).thenReturn(issueMock);

		// Act
		final var result = jiraClient.createIssue(projectKey, type, labels, summary, description);

		// Assert
		assertThat(result).isNotNull().isEqualTo(key);

		verify(jiraClientMock).getIssueApi();
		verify(issueMock).getKey();
		verify(issueApiMock).addIssue(any(Issue.class));
		verify(completableFutureIdentityMock).get();
	}

	@Test
	void createIssueUsingImplicitProjectKey() throws Exception {

		final var type = "Bug";
		final var summary = "Test issue";
		final var description = "Test description";
		final var key = "TEST-1";
		final var labels = List.of("my-label");

		when(issueMock.getKey()).thenReturn(key);
		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.addIssue(any())).thenReturn(completableFutureIdentityMock);
		when(completableFutureIdentityMock.get()).thenReturn(issueMock);

		// Act
		final var result = jiraClient.createIssue(type, labels, summary, description);

		// Assert
		assertThat(result).isNotNull().isEqualTo(key);

		verify(jiraClientMock).getIssueApi();
		verify(issueMock).getKey();
		verify(issueApiMock).addIssue(any(Issue.class));
		verify(completableFutureIdentityMock).get();
	}

	@Test
	void createIssueThrowsException() {

		// Arrange
		final var projectKey = "TEST";
		final var type = "Bug";
		final var summary = "Test issue";
		final var description = "Test description";
		final var labels = List.of("my-label");

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.addIssue(any())).thenThrow(new RuntimeException("Error"));

		// Act
		final var exception = assertThrows(JiraIntegrationException.class, () -> jiraClient.createIssue(projectKey, type, labels, summary, description));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("java.lang.RuntimeException: Error");

		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).addIssue(any(Issue.class));
	}

	@Test
	void getIssue() throws InterruptedException, ExecutionException {

		// Arrange
		final var projectKey = "TEST";
		final var issueTypeName = "Bug";
		final var issueDescription = "Test description";
		final var issueKey = "TEST-1";

		final var fields = new Fields();
		fields.setProject(Project.fromKey(projectKey));
		fields.setIssueType(IssueType.fromName(issueTypeName));
		fields.setDescription(issueDescription);

		when(issueMock.getKey()).thenReturn(issueKey);
		when(issueMock.getFields()).thenReturn(fields);
		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.getIssue(issueKey)).thenReturn(completableFutureIssueMock);
		when(completableFutureIssueMock.get()).thenReturn(issueMock);

		// Act
		final var result = jiraClient.getIssue(issueKey);

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getKey()).isEqualTo(issueKey);
		assertThat(result.get().getFields()).isEqualTo(fields);

		verify(jiraClientMock).getIssueApi();
		verify(issueMock).getKey();
		verify(issueApiMock).getIssue(issueKey);
		verify(completableFutureIssueMock).get();
	}

	@Test
	void getIssueWhenNotFound() {

		// Arrange
		final var issueKey = "TEST-1";

		when(jiraClientMock.getIssueApi()).thenThrow(new RuntimeException("Error occured"));

		// Act
		final var result = jiraClient.getIssue(issueKey);

		// Assert
		assertThat(result).isEmpty();
		verify(jiraClientMock).getIssueApi();
	}

	@Test
	void updateIssue() {

		// Arrange
		final var projectKey = "TEST";
		final var issueTypeName = "Bug";
		final var issueSummary = "Test issue";
		final var issue = Issue.from(issueTypeName, projectKey, issueSummary);

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);

		// Act
		jiraClient.updateIssue(issue);

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).updateIssue(issue);
	}

	@Test
	void addComment() {

		// Arrange
		final var commentBody = "Test comment";
		final var issueKey = "TEST-1";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);

		// Act
		jiraClient.addComment(issueKey, commentBody);

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).addComment(issueKey, Comment.from(commentBody));
	}

	@Test
	void addCommentWhenNoTextProvided() {

		// Arrange
		final var commentBody = "";
		final var issueKey = "TEST-1";

		// Act
		jiraClient.addComment(issueKey, commentBody);

		// Assert
		verifyNoInteractions(jiraClientMock, issueApiMock);
	}

	@Test
	void deleteComment() {

		// Arrange
		final var commentId = "666";
		final var issueKey = "TEST-1";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);

		// Act
		jiraClient.deleteComment(issueKey, commentId);

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).deleteComment(issueKey, commentId);
	}

	@Test
	void addAttachment() throws Exception {

		// Arrange
		final var issueKey = "TEST-1";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.addAttachment(any(), any())).thenReturn(completableFutureAttachmentsMock);

		// Act
		jiraClient.addAttachment(issueKey, file);

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).addAttachment(issueKey, file);
		verify(completableFutureAttachmentsMock).get();
	}

	@Test
	void addAttachmentThrowsException() {

		// Arrange
		final var issueKey = "TEST-1";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.addAttachment(any(), any())).thenThrow(new RuntimeException("Error"));

		// Act
		final var exception = assertThrows(JiraIntegrationException.class, () -> jiraClient.addAttachment(issueKey, file));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("java.lang.RuntimeException: Error");

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).addAttachment(issueKey, file);
	}

	@Test
	void deleteAttachment() {

		// Arrange
		final var attachmentId = "id";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);

		// Act
		jiraClient.deleteAttachment(attachmentId);

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).deleteAttachment(attachmentId);
	}

	@Test
	void getAttachment() throws Exception {

		// Arrange
		final var contentUrl = "contentUrl";
		final var attachmentContent = "This is a test attachment";
		final var byteArrayInputStream = new ByteArrayInputStream(attachmentContent.getBytes());

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.getAttachmentContent(any())).thenReturn(completableFutureInputStreamMock);
		when(completableFutureInputStreamMock.get()).thenReturn(byteArrayInputStream);

		// Act
		final var result = jiraClient.getAttachment(contentUrl);

		// Assert
		assertThat(result).isEqualTo(Base64.getEncoder().encodeToString(attachmentContent.getBytes()));

		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).getAttachmentContent(contentUrl);
		verify(completableFutureInputStreamMock).get();
	}

	@Test
	void getAttachmentThrowsException() {

		// Arrange
		final var contentUrl = "contentUrl";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.getAttachmentContent(any())).thenThrow(new RuntimeException("Error"));

		// Act
		final var exception = assertThrows(JiraIntegrationException.class, () -> jiraClient.getAttachment(contentUrl));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("java.lang.RuntimeException: Error");

		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).getAttachmentContent(contentUrl);
	}

	@Test
	void getTransitionsByIssue() throws Exception {

		// Arrange
		final var issueKey = "TEST-1";
		final var transitions = new Transitions();

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.getTransitions(issueKey)).thenReturn(completableFutureTransitionsMock);
		when(completableFutureTransitionsMock.get()).thenReturn(transitions);

		// Act
		jiraClient.getTransitions(issueKey);

		// Assert
		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).getTransitions(issueKey);
		verify(completableFutureTransitionsMock).get();
	}

	@Test
	void getTransitionsByIssueThrowsException() {

		// Arrange
		final var issueKey = "TEST-1";

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.getTransitions(issueKey)).thenThrow(new RuntimeException("Error"));

		// Act
		final var exception = assertThrows(JiraIntegrationException.class, () -> jiraClient.getTransitions(issueKey));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("java.lang.RuntimeException: Error");

		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).getTransitions(issueKey);
	}
}
