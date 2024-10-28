package se.sundsvall.incidentmapper.apptest;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
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

	private static final String POB_ISSUE_KEY = "12345";
	private static final String JIRA_ISSUE_KEY = "UF-5974";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/%s/incidents".formatted(MUNICIPALITY_ID);
	private static final String REQUEST_FILE = "request.json";
	private static final int WAIT_IN_SECONDS = 9;

	@Autowired
	private IncidentRepository incidentRepository;

	@Test
	void test01_newIssueFromPob() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withMaxVerificationDelayInSeconds(WAIT_IN_SECONDS)
			.sendRequest()
			.verifyStubs();
	}

	@Test
	void test02_updatedIssueFromPob() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey(POB_ISSUE_KEY)
			.withJiraIssueKey(JIRA_ISSUE_KEY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withStatus(SYNCHRONIZED));

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.withMaxVerificationDelayInSeconds(WAIT_IN_SECONDS)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_updatedIssueFromJira() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey(POB_ISSUE_KEY)
			.withJiraIssueKey(JIRA_ISSUE_KEY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withStatus(JIRA_INITIATED_EVENT));

		// Not necessary for this test case, but must be called in order to call verifyStubs().
		// The updatePob-logic is a scheduled job and is not depending on any user inputs to this micro-service.
		setupCall()
			.withServicePath("/api-docs")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withMaxVerificationDelayInSeconds(WAIT_IN_SECONDS)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_issueDoneInJira() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey(POB_ISSUE_KEY)
			.withJiraIssueKey(JIRA_ISSUE_KEY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLastSynchronizedJira(now())
			.withLastSynchronizedPob(now(systemDefault()))
			.withStatus(SYNCHRONIZED));

		assertThat(incidentRepository.findByMunicipalityIdAndPobIssueKey(MUNICIPALITY_ID, POB_ISSUE_KEY))
			.isPresent();

		// Not necessary for this test case, but must be called in order to call verifyStubs().
		// The updatePob-logic is a scheduled job and is not depending on any user inputs to this micro-service.
		setupCall()
			.withServicePath("/api-docs")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withMaxVerificationDelayInSeconds(WAIT_IN_SECONDS)
			.sendRequestAndVerifyResponse();

		assertThat(incidentRepository.findByMunicipalityIdAndPobIssueKey(MUNICIPALITY_ID, POB_ISSUE_KEY))
			.isNotPresent();
	}
}
