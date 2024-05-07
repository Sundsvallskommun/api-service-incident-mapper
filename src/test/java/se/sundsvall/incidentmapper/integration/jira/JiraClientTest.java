package se.sundsvall.incidentmapper.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.AttachmentInput;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.atlassian.util.concurrent.Promise;


@ExtendWith(MockitoExtension.class)
class JiraClientTest {


	@Mock
	private JiraRestClient jiraRestClientMock;

	@Mock
	private BasicIssue basicIssueMock;

	@Mock
	private Promise<BasicIssue> promiseMock;

	@Mock
	private Promise<Iterable<IssueType>> promiseIssueTypeMock;

	@Mock
	private IssueRestClient issueRestClientMock;

	@Mock
	private MetadataRestClient metadataRestClientMock;

	@Mock
	private Issue issueMock;

	@Mock
	private Transition transitionMock;

	@Mock
	private Promise<Issue> promiseIssueMock;

	@Mock
	private Promise<Void> promiseVoidMock;

	@Mock
	private Promise<Iterable<Transition>> promiseTransitionMock;

	@Mock
	private AttachmentInput attachmentInputMock;


	@InjectMocks
	private JiraClient jiraClient;

	@Test
	void createIssue() {

		// Arrange
		final var projectKey = "TEST";
		final var issueTypeName = "Bug";
		final var issueSummary = "Test issue";
		final var issueDescription = "Test description";
		final var issueKey = "TEST-1";

		final var self = URI.create("http://example.com");
		final var id = 1L;
		final var name = "Bug";
		final var isSubtask = false;
		final var description = "This is a test issue type";
		final var iconUri = URI.create("http://example.com/icon.png");
		final var issueType = new IssueType(self, id, name, isSubtask, description, iconUri);
		final List<IssueType> issueTypes = new ArrayList<>();
		issueTypes.add(issueType);

		//Mock
		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(issueRestClientMock.createIssue(any())).thenReturn(promiseMock);
		when(promiseMock.claim()).thenReturn(basicIssueMock);
		when(basicIssueMock.getKey()).thenReturn(issueKey);
		// Mock IssueType call
		when(jiraRestClientMock.getMetadataClient()).thenReturn(metadataRestClientMock);
		when(metadataRestClientMock.getIssueTypes()).thenReturn(promiseIssueTypeMock);
		when(promiseIssueTypeMock.claim()).thenReturn(issueTypes);

		// Act
		final var result = jiraClient.createIssue(projectKey, issueTypeName, issueSummary, issueDescription);
		// Assert
		assertThat(result).isNotNull().isEqualTo(issueKey);
	}

	@Test
	void getIssue() {
		// Arrange
		final var issueKey = "TEST-1";
		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(jiraRestClientMock.getIssueClient().getIssue(issueKey)).thenReturn(promiseIssueMock);
		when(promiseIssueMock.claim()).thenReturn(issueMock);

		// Act
		final var result = jiraClient.getIssue(issueKey);

		// Assert
		assertThat(result).isNotNull().isEqualTo(issueMock);
	}

	@Test
	void updateIssueDescription() {
		// Arrange
		final var issueKey = "TEST-1";
		final var newDescription = "New description";

		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(jiraRestClientMock.getIssueClient().updateIssue(any(), any())).thenReturn(promiseVoidMock);
		// Act
		jiraClient.updateIssueDescription(issueKey, newDescription);

		// Assert
		verify(jiraRestClientMock.getIssueClient()).updateIssue(any(), any());
	}

	@Test
	void updateIssueWithAttachments() {
		// Arrange
		final var issueKey = "TEST-1";
		final var attachments = List.of(attachmentInputMock);
		final var attachmentUri = URI.create("http://example.com");

		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(jiraRestClientMock.getIssueClient().getIssue(issueKey)).thenReturn(promiseIssueMock);
		when(promiseIssueMock.claim()).thenReturn(issueMock);
		when(issueMock.getAttachmentsUri()).thenReturn(attachmentUri);
		when(jiraRestClientMock.getIssueClient().addAttachments(any(URI.class), any(AttachmentInput.class))).thenReturn(promiseVoidMock);

		// Act
		jiraClient.updateIssueWithAttachments(issueKey, attachments);

		// Assert
		verify(jiraRestClientMock.getIssueClient()).addAttachments(any(URI.class), any(AttachmentInput.class));
	}

	@Test
	void updateIssueStatus() {
		// Arrange
		final var newStatus = "Done";

		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(jiraRestClientMock.getIssueClient().getTransitions(issueMock)).thenReturn(promiseTransitionMock);
		when(promiseTransitionMock.claim()).thenReturn(List.of(transitionMock));
		when(transitionMock.getName()).thenReturn(newStatus);
		when(jiraRestClientMock.getIssueClient().transition(any(Issue.class), any(TransitionInput.class))).thenReturn(promiseVoidMock);

		// Act
		jiraClient.updateIssueStatus(issueMock, newStatus);

		// Assert
		verify(jiraRestClientMock.getIssueClient()).transition(any(Issue.class), any(TransitionInput.class));
	}

	@Test
	void deleteIssue() {
		// Arrange
		final var issueKey = "TEST-1";
		final var deleteSubtasks = true;

		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(jiraRestClientMock.getIssueClient().deleteIssue(issueKey, deleteSubtasks)).thenReturn(promiseVoidMock);

		// Act
		jiraClient.deleteIssue(issueKey, deleteSubtasks);

		// Assert
		verify(jiraRestClientMock.getIssueClient()).deleteIssue(issueKey, deleteSubtasks);
	}

	@Test
	void addComment() {
		// Arrange
		final var commentBody = "Test comment";
		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(jiraRestClientMock.getIssueClient().addComment(issueMock.getCommentsUri(), Comment.valueOf(commentBody))).thenReturn(promiseVoidMock);

		// Act
		jiraClient.addComment(issueMock, commentBody);

		// Assert
		verify(jiraRestClientMock.getIssueClient()).addComment(issueMock.getCommentsUri(), Comment.valueOf(commentBody));
	}

}
