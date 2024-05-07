package se.sundsvall.incidentmapper.integration.jira;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.AttachmentInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;


@Component
public class JiraClient {

	private final JiraRestClient restClient;

	private final List<IssueType> issueTypeCache = new ArrayList<>();

	public JiraClient(final JiraRestClient restClient) {
		this.restClient = restClient;
	}

	public Issue getIssue(final String issueKey) {

		return restClient.getIssueClient().getIssue(issueKey).claim();
	}

	public String createIssue(final String projectKey, final String issueType, final String issueSummary, final String description) {

		final var issueTypeId = getIssueTypeByName(issueType).getId();

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

		final var transitionId = getStatusByName(issue, newStatus);
		restClient.getIssueClient().transition(issue, new TransitionInput(transitionId)).claim();
	}

	public void deleteIssue(final String issueKey, final boolean deleteSubtasks) {
		restClient.getIssueClient().deleteIssue(issueKey, deleteSubtasks).claim();
	}

	public void addComment(final Issue issue, final String commentBody) {
		restClient.getIssueClient().addComment(issue.getCommentsUri(), Comment.valueOf(commentBody));
	}

	private IssueType getIssueTypeByName(final String name) {

		if (issueTypeCache.isEmpty()) {
			final var test = restClient.getMetadataClient().getIssueTypes().claim();
			test.forEach(issueTypeCache::add);
		}
		return issueTypeCache.stream()
			.filter(issueType -> issueType.getName().equalsIgnoreCase(name))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "No issue type found with name: " + name));
	}

	private Iterable<Transition> getTransitions(final Issue issue) {
		return restClient.getIssueClient().getTransitions(issue).claim();
	}

	private int getStatusByName(final Issue issue, final String name) {
		return StreamSupport.stream(getTransitions(issue).spliterator(), false)
			.filter(trans -> trans.getName().equals(name))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Transition not found"))
			.getId();
	}

}
