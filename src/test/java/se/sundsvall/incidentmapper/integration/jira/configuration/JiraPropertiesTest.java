package se.sundsvall.incidentmapper.integration.jira.configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.incidentmapper.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class JiraPropertiesTest {

	@Autowired
	private JiraProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.password()).isEqualTo("some-password");
		assertThat(properties.username()).isEqualTo("some-username");
		assertThat(properties.url()).isEqualTo("http://jira.url");
	}

}
