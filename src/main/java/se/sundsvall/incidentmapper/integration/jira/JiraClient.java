package se.sundsvall.incidentmapper.integration.jira;

import static java.util.Optional.empty;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.AttachmentInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import org.springframework.stereotype.Component;

@Component
public class JiraClient {

	private final JiraRestClient restClient;

	private final JiraUtil jiraUtil;

	public JiraClient(final JiraRestClient restClient, final JiraUtil jiraUtil) {
		this.restClient = restClient;
		this.jiraUtil = jiraUtil;
	}

	public Optional<Issue> getIssue(final String issueKey) {
		try {
			return Optional.of(restClient.getIssueClient().getIssue(issueKey).claim());
		} catch (final Exception e) {
			return empty();
		}
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
		final IssueInput input = new IssueInputBuilder().setDescription(newDescription).build();
		restClient.getIssueClient().updateIssue(issueKey, input).claim();
	}

	public InputStream getAttachment(final URI attachmentURI) {
		return restClient.getIssueClient().getAttachment(attachmentURI).claim();
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
