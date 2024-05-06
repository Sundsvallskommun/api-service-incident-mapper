package se.sundsvall.incidentmapper.integration.db.listener;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

public class IncidentEntityListener {

	@PrePersist
	void prePersist(final IncidentEntity entity) {
		entity.setCreated(now(systemDefault()).truncatedTo(MILLIS));
	}

	@PreUpdate
	void preUpdate(final IncidentEntity entity) {
		entity.setModified(now(systemDefault()).truncatedTo(MILLIS));
	}
}
