package se.sundsvall.incidentmapper.api;

import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.incidentmapper.Application;
import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.service.IncidentService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class IncidentResourceTest {

	private static final String PATH = "/{municipalityId}/incidents";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private IncidentService incidentServiceMock;

	@Test
	void postIncident() {

		// Arrange
		final var municipalityId = "2281";
		final var incidentKey = "INCIDENT-12345";
		final var body = IncidentRequest.create()
			.withIncidentKey(incidentKey);

		// Act
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", municipalityId)))
			.bodyValue(body)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		// Assert
		verify(incidentServiceMock).handleIncidentRequest(municipalityId, body);
	}
}
