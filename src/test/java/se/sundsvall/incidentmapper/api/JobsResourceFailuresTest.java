package se.sundsvall.incidentmapper.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.incidentmapper.Application;
import se.sundsvall.incidentmapper.service.scheduler.SynchronizerSchedulerService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class JobsResourceFailuresTest {

	private static final String PATH = "/{municipalityId}/jobs";
	private static final String MUNICIPALITY_ID = "invalid-municipality-id";

	@MockitoBean
	private SynchronizerSchedulerService synchronizerSchedulerServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void synchronizerInvalidMunicipalityId() {
		var response = webTestClient.post()
			.uri(builder -> builder.path(PATH + "/synchronizer")
				.build(Map.of("municipalityId", MUNICIPALITY_ID)))
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
			.containsExactlyInAnyOrder(tuple("synchronizer.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(synchronizerSchedulerServiceMock);
	}

}
