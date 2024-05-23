package se.sundsvall.incidentmapper.integration.jira;

import static java.util.Optional.empty;

import java.io.File;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.chavaillaz.client.jira.JiraClient;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Issue;
import com.chavaillaz.client.jira.domain.Status;

import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;

@Component
public class JiraIncidentClient {

	private final JiraClient<Issue> jiraClient;
	private final JiraProperties jiraProperties;

	public JiraIncidentClient(final JiraClient<Issue> jiraClient, JiraProperties jiraProperties) {
		this.jiraClient = jiraClient;
		this.jiraProperties = jiraProperties;
	}

	public JiraProperties getProperties() {
		return this.jiraProperties;
	}

	public Optional<Issue> getIssue(final String issueKey) {
		try {
			return Optional.of(jiraClient.getIssueApi().getIssue(issueKey).get());
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			return empty();
		}
	}

	public String createIssue(final String issueType, final String issueSummary, final String description, final Status initialStatus) {
		return createIssue(jiraProperties.projectKey(), issueType, issueSummary, description, initialStatus);
	}

	public String createIssue(final String projectKey, final String issueType, final String issueSummary, final String description, final Status initialStatus) {

		final var issue = Issue.from(issueType, projectKey, issueSummary);
		issue.getFields().setStatus(initialStatus);
		issue.getFields().setDescription(description);

		try {
			return jiraClient.getIssueApi().addIssue(issue).get().getKey();
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	public void updateIssue(Issue issue) {
		jiraClient.getIssueApi().updateIssue(issue);
	}

	public void addComment(String issueKey, String commentBody) {
		jiraClient.getIssueApi().addComment(issueKey, Comment.from(commentBody));
	}

	public void deleteComment(String issueKey, String commentId) {
		jiraClient.getIssueApi().deleteComment(issueKey, commentId);
	}

	public void addAttachment(String issueKey, File file) {
		try {
			jiraClient.getIssueApi().addAttachment(issueKey, file).get();
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	public String getAttachment(String contentUrl) {
		try {
			return Base64.getEncoder().encodeToString(jiraClient.getIssueApi().getAttachmentContent(contentUrl).get().readAllBytes());
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	public void deleteAttachment(String attachmentId) {
		jiraClient.getIssueApi().deleteAttachment(attachmentId);
	}
}
