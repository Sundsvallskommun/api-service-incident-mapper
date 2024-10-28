package se.sundsvall.incidentmapper.apptest;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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
	private static final String INCIDENTS_PATH = "/%s/incidents".formatted(MUNICIPALITY_ID);
	private static final String SYNCHRONIZER_PATH = "/%s/jobs/synchronizer".formatted(MUNICIPALITY_ID);
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private IncidentRepository incidentRepository;

	@Test
	void test01_newIssueFromPob() {
		setupCall()
			.withServicePath(INCIDENTS_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest();

		setupCall()
			.withServicePath(SYNCHRONIZER_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_updatedIssueFromPob() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey(POB_ISSUE_KEY)
			.withJiraIssueKey(JIRA_ISSUE_KEY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withStatus(SYNCHRONIZED));

		setupCall()
			.withServicePath(INCIDENTS_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest();

		setupCall()
			.withServicePath(SYNCHRONIZER_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_updatedIssueFromJira() {

		incidentRepository.saveAndFlush(IncidentEntity.create()
			.withPobIssueKey(POB_ISSUE_KEY)
			.withJiraIssueKey(JIRA_ISSUE_KEY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withStatus(JIRA_INITIATED_EVENT));

		setupCall()
			.withServicePath(SYNCHRONIZER_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
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

		setupCall()
			.withServicePath(SYNCHRONIZER_PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(incidentRepository.findByMunicipalityIdAndPobIssueKey(MUNICIPALITY_ID, POB_ISSUE_KEY))
			.isNotPresent();
	}
}
