package se.sundsvall.incidentmapper.integration.jira;

import java.util.List;

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

	public Issue getIssue(final String issueKey) {

		return restClient.getIssueClient().getIssue(issueKey).claim();
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

	public void updateIssueWithAttachments(final String issueKey, final List<AttachmentInput> attachments) {

		final var attachmentsArray = new AttachmentInput[attachments.size()];
		attachments.toArray(attachmentsArray);

		final var attachmentUri = getIssue(issueKey).getAttachmentsUri();
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