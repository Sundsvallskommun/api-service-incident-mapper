package se.sundsvall.incidentmapper.integration.jira;

import static java.util.Optional.empty;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import com.chavaillaz.client.jira.JiraClient;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Issue;

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
			return empty();
		}
	}

	public String createIssue(final String issueType, final String issueSummary, final String description) {
		return createIssue(jiraProperties.projectKey(), issueType, issueSummary, description);
	}

	public String createIssue(final String projectKey, final String issueType, final String issueSummary, final String description) {

		final var issue = Issue.from(issueType, projectKey, issueSummary);
		issue.getFields().setDescription(description);

		try {
			return jiraClient.getIssueApi().addIssue(issue).get().getKey();
		} catch (final Exception e) {
			e.printStackTrace();
			throw Problem.valueOf(BAD_REQUEST, e.getMessage());
		}
	}

	public void updateIssue(Issue issue) {
		jiraClient.getIssueApi().updateIssue(issue);
	}

	public void addComment(String issueKey, String commentBody) {
		jiraClient.getIssueApi().addComment(issueKey, Comment.from(commentBody));
	}

	public String getAttachment(String attachmentId) {
		try {
			return jiraClient.getIssueApi().getAttachment(attachmentId).get().getContent();
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw Problem.valueOf(BAD_REQUEST, e.getMessage());
		}
	}
}
