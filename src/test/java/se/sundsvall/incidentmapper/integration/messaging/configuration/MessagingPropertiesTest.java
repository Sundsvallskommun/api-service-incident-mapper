package se.sundsvall.incidentmapper.integration.messaging.configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.incidentmapper.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MessagingPropertiesTest {

	@Autowired
	private MessagingProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.channel()).isEqualTo("the-channel");
		assertThat(properties.token()).isEqualTo("the-token");
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
