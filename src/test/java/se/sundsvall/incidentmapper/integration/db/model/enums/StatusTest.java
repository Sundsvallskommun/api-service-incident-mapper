package se.sundsvall.incidentmapper.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.CLOSED;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;

import org.junit.jupiter.api.Test;

class StatusTest {

	@Test
	void enums() {
		assertThat(Status.values()).containsExactlyInAnyOrder(
			SYNCHRONIZED, POB_INITIATED_EVENT, JIRA_INITIATED_EVENT, CLOSED);
	}
}
