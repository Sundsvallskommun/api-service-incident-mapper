package se.sundsvall.incidentmapper.integration.jira;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.stream.StreamSupport;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Transition;

@Component
class JiraUtil {

	private final JiraRestClient restClient;

	public JiraUtil(final JiraRestClient restClient) {
		this.restClient = restClient;
	}

	@Cacheable("issueTypes")
	public IssueType getIssueTypeByName(final String name) {
		final var issueTypes = restClient.getMetadataClient().getIssueTypes().claim();
		return StreamSupport.stream(issueTypes.spliterator(), false)
			.filter(issueType -> issueType.getName().equalsIgnoreCase(name))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "No issue type found with name: " + name));
	}

	public int getTransitionByName(final Issue issue, final String name) {
		return StreamSupport.stream(getTransitions(issue).spliterator(), false)
			.filter(trans -> name.equals(trans.getName()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Transition not found"))
			.getId();
	}

	private Iterable<Transition> getTransitions(final Issue issue) {
		return restClient.getIssueClient().getTransitions(issue).claim();
	}

}
