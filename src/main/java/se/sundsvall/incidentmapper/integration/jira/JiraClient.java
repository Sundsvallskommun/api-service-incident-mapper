package se.sundsvall.incidentmapper.integration.jira;

import static java.util.Optional.empty;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.AttachmentInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;

import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;

@Component
public class JiraClient {

	private final JiraRestClient restClient;
	private final JiraUtil jiraUtil;
	private final JiraProperties jiraProperties;

	public JiraClient(final JiraRestClient restClient, final JiraUtil jiraUtil, JiraProperties jiraProperties) {
		this.restClient = restClient;
		this.jiraUtil = jiraUtil;
		this.jiraProperties = jiraProperties;
	}

	public Optional<Issue> getIssue(final String issueKey) {
		try {
			return Optional.of(restClient.getIssueClient().getIssue(issueKey).claim());
		} catch (final Exception e) {
			return empty();
		}
	}

	public String createIssue(final String issueType, final String issueSummary, final String description) {
		return createIssue(jiraProperties.projectKey(), issueType, issueSummary, description);
	}

	public String createIssue(final String projectKey, final String issueType, final String issueSummary, final String description) {
		final var issueTypeId = jiraUtil.getIssueTypeByName(issueType).getId();
		final var newIssue = new IssueInputBuilder()
			.setProjectKey(projectKey)
			.setIssueTypeId(issueTypeId)
			.setSummary(issueSummary)
			.setDescription(description)
			.build();

		return restClient.getIssueClient().createIssue(newIssue).claim().getKey();
	}

	public void updateIssueDescription(final String issueKey, final String newDescription) {
		final var input = new IssueInputBuilder().setDescription(newDescription).build();
		restClient.getIssueClient().updateIssue(issueKey, input).claim();
	}

	public void updateIssueWithAttachments(final String issueKey, final List<AttachmentInput> attachments) {
		final var attachmentsArray = new AttachmentInput[attachments.size()];
		attachments.toArray(attachmentsArray);

		final var attachmentUri = getIssue(issueKey).get().getAttachmentsUri();
		restClient.getIssueClient().addAttachments(attachmentUri, attachmentsArray).claim();
	}

	public void updateIssueStatus(final Issue issue, final String newStatus) {
		final var transitionId = jiraUtil.getTransitionByName(issue, newStatus);
		restClient.getIssueClient().transition(issue, new TransitionInput(transitionId)).claim();
	}

	public void deleteIssue(final String issueKey, final boolean deleteSubtasks) {
		restClient.getIssueClient().deleteIssue(issueKey, deleteSubtasks).claim();
	}

	public void addComment(final Issue issue, final String commentBody) {
		restClient.getIssueClient().addComment(issue.getCommentsUri(), Comment.valueOf(commentBody));
	}
}
