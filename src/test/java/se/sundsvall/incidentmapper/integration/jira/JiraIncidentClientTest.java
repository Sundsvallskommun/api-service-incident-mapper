package se.sundsvall.incidentmapper.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavaillaz.client.jira.JiraClient;
import com.chavaillaz.client.jira.api.IssueApi;
import com.chavaillaz.client.jira.domain.Attachment;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Fields;
import com.chavaillaz.client.jira.domain.Identity;
import com.chavaillaz.client.jira.domain.Issue;
import com.chavaillaz.client.jira.domain.IssueType;
import com.chavaillaz.client.jira.domain.Project;

import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;

@ExtendWith(MockitoExtension.class)
class JiraIncidentClientTest {

	@Mock
	private Issue issueMock;

	@Mock
	private IssueApi<Issue> issueApiMock;

	@Mock
	private JiraClient<Issue> jiraClientMock;

	@Mock
	private JiraProperties jiraPropertiesMock;

	@Mock
	private CompletableFuture<Identity> completableFutureIdentityMock;

	@Mock
	private CompletableFuture<Issue> completableFutureIssueMock;

	@Mock
	private CompletableFuture<Attachment> completableFutureAttachmentMock;

	@InjectMocks
	private JiraIncidentClient jiraClient;

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
		final var issueTypeName = "Bug";
		final var issueSummary = "Test issue";
		final var issueDescription = "Test description";
		final var issueKey = "TEST-1";

		when(issueMock.getKey()).thenReturn(issueKey);
		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.addIssue(any())).thenReturn(completableFutureIdentityMock);
		when(completableFutureIdentityMock.get()).thenReturn(issueMock);

		// Act
		final var result = jiraClient.createIssue(projectKey, issueTypeName, issueSummary, issueDescription);

		// Assert
		assertThat(result).isNotNull().isEqualTo(issueKey);

		verify(jiraClientMock).getIssueApi();
		verify(issueMock).getKey();
		verify(issueApiMock).addIssue(any(Issue.class));
		verify(completableFutureIdentityMock).get();
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
	void getAttachment() throws InterruptedException, ExecutionException {

		final var content = "content";
		final var attachmentId = "666";
		final var attachment = new Attachment();
		attachment.setContent(content);

		when(jiraClientMock.getIssueApi()).thenReturn(issueApiMock);
		when(issueApiMock.getAttachment(attachmentId)).thenReturn(completableFutureAttachmentMock);
		when(completableFutureAttachmentMock.get()).thenReturn(attachment);

		// Act
		final var result = jiraClient.getAttachment(attachmentId);

		// Assert
		assertThat(result).isNotNull().isEqualTo(content);

		verify(jiraClientMock).getIssueApi();
		verify(issueApiMock).getAttachment(attachmentId);
		verify(completableFutureIssueMock).get();
	}
}
