package se.sundsvall.incidentmapper.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "Incident request model")
public class IncidentRequest {

	@Schema(description = "The incident key", example = "INCIDENT-12345", requiredMode = REQUIRED)

	@NotBlank(message = "a valid value must be provided")
	private String incidentKey;

	public static IncidentRequest create() {
		return new IncidentRequest();
	}

	public String getIncidentKey() {
		return incidentKey;
	}

	public void setIncidentKey(String incidentKey) {
		this.incidentKey = incidentKey;
	}

	public IncidentRequest withIncidentKey(String incidentKey) {
		this.incidentKey = incidentKey;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(incidentKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final IncidentRequest other)) { return false; }
		return Objects.equals(incidentKey, other.incidentKey);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("IncidentRequest [incidentKey=").append(incidentKey).append("]");
		return builder.toString();
	}
}
