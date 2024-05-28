package se.sundsvall.incidentmapper.apptest;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.incidentmapper.Application;

@WireMockAppTestSuite(files = "classpath:/IncidentsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
})
class IncidentsIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";

	@Test
	void test01_newIssue() {
		setupCall()
			.withServicePath("/incidents")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withMaxVerificationDelayInSeconds(5)
			.sendRequest()
			.verifyStubs();
	}
}
