package se.sundsvall.incidentmapper.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.incidentmapper.Application;
import se.sundsvall.incidentmapper.api.model.IncidentRequest;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class IncidentResourceFailuresTest {

	private static final String PATH = "/incidents";

	@Autowired
	private WebTestClient webTestClient;

	// TODO: Activate this when service-class is implemented.
	// @MockBean
	// private IncidentService incidentServiceMock;

	@Test
	void postIncidentBlankIssueKey() {

		// Arrange
		final var incidentKey = " "; // blank
		final var body = IncidentRequest.create()
			.withIncidentKey(incidentKey);

		// Act
		final var response = webTestClient.post()
			.uri(PATH)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("incidentKey", "a valid value must be provided"));

		// TODO: Activate this when service-class is implemented.
		// verifyNoInteractions(incidentServiceMock);
	}
}
