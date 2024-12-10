package se.sundsvall.incidentmapper.integration.jira.configuration;

import com.chavaillaz.client.jira.JiraClient;
import com.chavaillaz.client.jira.domain.Issue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JiraConfiguration {

	private final JiraProperties jiraProperties;

	public JiraConfiguration(final JiraProperties jiraProperties) {
		this.jiraProperties = jiraProperties;
	}

	@Bean
	JiraClient<Issue> jiraRestClient() {
		return JiraClient.javaClient(jiraProperties.url())
			.withUserAuthentication(jiraProperties.username(), jiraProperties.password());
	}
}
