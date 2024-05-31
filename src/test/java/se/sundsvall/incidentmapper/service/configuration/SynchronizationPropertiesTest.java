package se.sundsvall.incidentmapper.service.configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.incidentmapper.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class SynchronizationPropertiesTest {

	@Autowired
	private SynchronizationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.clockSkewInSeconds()).isEqualTo(15);
		assertThat(properties.responsibleUserGroupInPob()).isEqualTo("IT Support");
		assertThat(Path.of(properties.tempFolder()).endsWith(Path.of("target/tmp")));
	}
}
