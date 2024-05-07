package se.sundsvall.incidentmapper.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Transition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.atlassian.util.concurrent.Promise;

@ExtendWith(MockitoExtension.class)
class IssueTypeServiceTest {


	@Mock
	private JiraRestClient jiraRestClientMock;

	@Mock
	private MetadataRestClient metadataRestClientMock;

	@Mock
	private IssueRestClient issueRestClientMock;

	@Mock
	private Promise<Iterable<IssueType>> promiseMock;

	@Mock
	private Promise<Iterable<Transition>> promiseTransitionMock;

	@Mock
	private Transition transitionMock;

	@Mock
	private Issue issueMock;

	@InjectMocks
	private JiraUtil jiraUtil;

	@Test
	void getIssueTypeByName() {

		// Arrange
		final var self = URI.create("https://example.com");
		final var id = 1L;
		final var name = "Bug";
		final var isSubtask = false;
		final var description = "This is a test issue type";
		final var iconUri = URI.create("https://example.com/icon.png");
		final var issueType = new IssueType(self, id, name, isSubtask, description, iconUri);
		final var issueTypes = List.of(issueType);

		when(jiraRestClientMock.getMetadataClient()).thenReturn(metadataRestClientMock);
		when(metadataRestClientMock.getIssueTypes()).thenReturn(promiseMock);
		when(promiseMock.claim()).thenReturn(issueTypes);

		// Act
		final var result = jiraUtil.getIssueTypeByName(name);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(name);
	}


	@Test
	void getTransitionByName() {
		// Arrange
		final var id = 1;
		final var name = "In Progress";
		when(jiraRestClientMock.getIssueClient()).thenReturn(issueRestClientMock);
		when(issueRestClientMock.getTransitions(issueMock)).thenReturn(promiseTransitionMock);
		when(promiseTransitionMock.claim()).thenReturn(List.of(transitionMock));
		when(transitionMock.getName()).thenReturn(name);
		when(transitionMock.getId()).thenReturn(id);
		// Act
		final var result = jiraUtil.getTransitionByName(issueMock, name);

		// Assert
		assertThat(result).isEqualTo(id);
	}

}
