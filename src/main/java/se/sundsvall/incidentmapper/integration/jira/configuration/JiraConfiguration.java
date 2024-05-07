package se.sundsvall.incidentmapper.integration.jira.configuration;

import java.net.URI;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JiraConfiguration {

	private final JiraProperties jiraProperties;

	public JiraConfiguration(final JiraProperties jiraProperties) {
		this.jiraProperties = jiraProperties;
	}

	@Bean
	JiraRestClient jiraRestClient() {
		return new AsynchronousJiraRestClientFactory()
			.createWithBasicHttpAuthentication(getJiraUri(), jiraProperties.username(), jiraProperties.password());
	}

	private URI getJiraUri() {
		return URI.create(jiraProperties.url());
	}

}
