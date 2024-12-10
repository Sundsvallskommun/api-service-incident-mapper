package se.sundsvall.incidentmapper.integration.jira;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;

import com.chavaillaz.client.common.exception.ResponseException;
import com.chavaillaz.client.jira.JiraClient;
import com.chavaillaz.client.jira.domain.Comment;
import com.chavaillaz.client.jira.domain.Issue;
import com.chavaillaz.client.jira.domain.IssueTransition;
import com.chavaillaz.client.jira.domain.Transition;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import se.sundsvall.incidentmapper.integration.jira.configuration.JiraProperties;

@Component
@CircuitBreaker(name = "jiraIntegration")
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

	/**
	 * Fetch Jira issue, as an Optional.
	 *
	 * @param  issueKey the Jira issue key
	 * @return          the issue as an Optional (Optional.empty if it doesn't exist)
	 */
	public Optional<Issue> getIssue(final String issueKey) {
		try {
			return Optional.of(jiraClient.getIssueApi().getIssue(issueKey).get());
		} catch (final Exception e) {
			if ((e.getCause() instanceof final ResponseException responseException) && responseException.getStatusCode().equals(NOT_FOUND.value())) {
				return empty();
			}
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	/**
	 * Create a Jira issue, with the configured default project key.
	 *
	 * @param type        the issue type (e.g. "Bug")
	 * @param labels      the issue labels (e.g. "my-label")
	 * @param summary     the issue summary (subject)
	 * @param description the issue description.
	 */
	public String createIssue(final String type, List<String> labels, final String summary, final String description) {
		return createIssue(jiraProperties.projectKey(), type, labels, summary, description);
	}

	/**
	 * Create a Jira issue.
	 *
	 * @param projectKey  the project key.
	 * @param type        the issue type (e.g. "Bug")
	 * @param labels      the issue labels (e.g. "my-label")
	 * @param summary     the issue summary (subject)
	 * @param description the issue description.
	 */
	public String createIssue(final String projectKey, final String type, List<String> labels, final String summary, final String description) {

		final var issue = Issue.from(type, projectKey, summary);
		issue.getFields().setDescription(description);
		issue.getFields().setLabels(labels);

		try {
			return jiraClient.getIssueApi().addIssue(issue).get().getKey();
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	/**
	 * Get available transitions for an issue.
	 *
	 * @param  issueKey the issue key.
	 * @return          a map of transitions where the keys are the transition name.
	 */
	public Map<String, Transition> getTransitions(String issueKey) {
		try {
			return jiraClient.getIssueApi().getTransitions(issueKey).get().stream()
				.collect(toMap(Transition::getName, Function.identity()));

		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	/**
	 * Perform a transition
	 *
	 * @param issue The issue containing only the fields to update.
	 */
	public void performTransition(String issueKey, Transition transition) {
		jiraClient.getIssueApi().doTransition(issueKey, IssueTransition.from(transition));
	}

	/**
	 * Update a Jira issue.
	 *
	 * @param issue The issue containing only the fields to update.
	 */
	public void updateIssue(Issue issue) {
		jiraClient.getIssueApi().updateIssue(issue);
	}

	/**
	 * Add a comment by jira issue key and comment body.
	 *
	 * @param issueKey    the Jira issue key
	 * @param commentBody the comment text string.
	 */
	public void addComment(String issueKey, String commentBody) {
		if (hasText(commentBody)) {
			jiraClient.getIssueApi().addComment(issueKey, Comment.from(commentBody));
		}
	}

	/**
	 * Delete a comment by Jira issue key and a commentId.
	 *
	 * @param issueKey  the Jira issue key
	 * @param commentId the comment ID.
	 */
	public void deleteComment(String issueKey, String commentId) {
		jiraClient.getIssueApi().deleteComment(issueKey, commentId);
	}

	/**
	 * Add an attachment by jira issue key and File object.
	 *
	 * @param issueKey the Jira issue key
	 * @param file     the attachment as a File object
	 */
	public void addAttachment(String issueKey, File file) {
		try {
			jiraClient.getIssueApi().addAttachment(issueKey, file).get();
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	/**
	 * Fetch the Jira issue attachment as a Base64 encoded string.
	 *
	 * @param  contentUrl the URL to the attachment content. (Content-URL is available in Attachment.getContent())
	 * @return            the attachment as a Base64 encoded string.
	 */
	public String getAttachment(String contentUrl) {
		try {
			return Base64.getEncoder().encodeToString(jiraClient.getIssueApi().getAttachmentContent(contentUrl).get().readAllBytes());
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			throw new JiraIntegrationException(e);
		}
	}

	/**
	 * Deletes an attachment by attachment ID.
	 *
	 * @param attachmentId the attachment ID.
	 */
	public void deleteAttachment(String attachmentId) {
		jiraClient.getIssueApi().deleteAttachment(attachmentId);
	}
}
