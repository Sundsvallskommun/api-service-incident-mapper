package se.sundsvall.incidentmapper.apptest;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.incidentmapper.Application;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

@WireMockAppTestSuite(files = "classpath:/IncidentsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
})
class IncidentsIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/%s/incidents".formatted(MUNICIPALITY_ID);
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private IncidentRepository incidentRepository;

	@Test
	void test01_newIssueFromPob() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withMaxVerificationDelayInSeconds(5)
			.sendRequest()
			.verifyStubs();
	}

	@Test
	void test02_updatedIssueFromPob() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey("12345")
			.withJiraIssueKey("UF-5974")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withStatus(SYNCHRONIZED));

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withMaxVerificationDelayInSeconds(5)
			.sendRequest()
			.verifyStubs();
	}

	@Test
	void test03_updatedIssueFromJira() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey("12345")
			.withJiraIssueKey("UF-5974")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withStatus(JIRA_INITIATED_EVENT));

		// Not necessary for this test case, but must be called in order to call verifyStubs().
		// The updatePob-logic is a scheduled job and is not depending on any user inputs to this micro-service.
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withMaxVerificationDelayInSeconds(5)
			.sendRequest()
			.verifyStubs();
	}
}
