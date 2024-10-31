package se.sundsvall.incidentmapper.integration.jira.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JiraConfigurationTest {

	@Mock
	private JiraProperties jiraPropertiesMock;

	@InjectMocks
	private JiraConfiguration jiraConfiguration;

	@Test
	void testJiraRestClient() {
		// Arrange
		when(jiraPropertiesMock.url()).thenReturn("http://jira.url");
		when(jiraPropertiesMock.username()).thenReturn("some-username");
		when(jiraPropertiesMock.password()).thenReturn("some-password");
		// Act
		final var jiraRestClient = jiraConfiguration.jiraRestClient();
		// Assert
		assertThat(jiraRestClient).isNotNull();
	}

}
