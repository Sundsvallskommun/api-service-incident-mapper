package se.sundsvall.incidentmapper.api;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.incidentmapper.Application;
import se.sundsvall.incidentmapper.api.model.IncidentRequest;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class IncidentResourceTest {

	private static final String PATH = "/incidents";

	@Autowired
	private WebTestClient webTestClient;

	// TODO: Activate this when service-class is implemented.
	// @MockBean
	// private IncidentService incidentServiceMock;

	@Test
	void postIncident() {

		// Arrange
		final var incidentKey = "INCIDENT-12345";
		final var body = IncidentRequest.create()
			.withIncidentKey(incidentKey);

		// Act
		webTestClient.post()
			.uri(PATH)
			.bodyValue(body)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		// Assert
		// TODO: Activate this when service-class is implemented.
		// verifyNoInteractions(incidentServiceMock);
	}
}
