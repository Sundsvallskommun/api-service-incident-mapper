package se.sundsvall.incidentmapper.api;

import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.incidentmapper.Application;
import se.sundsvall.incidentmapper.service.scheduler.SynchronizerSchedulerService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class JobsResourceTest {

	private static final String PATH = "/{municipalityId}/jobs";
	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private SynchronizerSchedulerService synchronizerSchedulerServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void synchronizer() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/synchronizer")
				.build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(synchronizerSchedulerServiceMock).execute();
	}

}
