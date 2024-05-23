package se.sundsvall.incidentmapper.integration.db.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status model", enumAsRef = true, example = "CLOSED")
public enum Status {
	SYNCHRONIZED,
	POB_INITIATED_EVENT,
	JIRA_INITIATED_EVENT;
}
