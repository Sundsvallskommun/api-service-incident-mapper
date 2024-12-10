package se.sundsvall.incidentmapper.integration.db.listener;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

class IncidentEntityListenerTest {

	@Test
	void prePersist() {

		// Arrange
		final var listener = new IncidentEntityListener();
		final var entity = new IncidentEntity();

		// Act
		listener.prePersist(entity);

		// Assert
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
		assertThat(entity.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void preUpdate() {

		// Arrange
		final var listener = new IncidentEntityListener();
		final var entity = new IncidentEntity();

		// Act
		listener.preUpdate(entity);

		// Assert
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
		assertThat(entity.getModified()).isCloseTo(now(), within(2, SECONDS));
	}
}
