package se.sundsvall.incidentmapper.integration.db;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.JIRA_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

/**
 * IncidentRepository tests.
 *
 * @see /src/test/resources/db/testdata-junit.sql for data setup.
 */
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class IncidentRepositoryTest {

	@Autowired
	private IncidentRepository repository;

	@Test
	void findByJiraIssueKey() {

		// Arrange
		final var jiraIssueKey = "JIR-007";

		// Act
		final var result = repository.findByJiraIssueKey(jiraIssueKey).orElseThrow();

		// Assert
		assertThat(result)
			.isNotNull()
			.extracting(IncidentEntity::getJiraIssueKey, IncidentEntity::getPobIssueKey, IncidentEntity::getStatus)
			.containsExactly("JIR-007", "POB-007", JIRA_INITIATED_EVENT);
	}

	@Test
	void findByJiraIssueKeyNotFound() {

		// Arrange
		final var jiraIssueKey = "non-existing";

		// Act
		final var result = repository.findByJiraIssueKey(jiraIssueKey);

		// Assert
		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void findByPobIssueKey() {

		// Arrange
		final var pobIssueKey = "POB-001";

		// Act
		final var result = repository.findByPobIssueKey(pobIssueKey).orElseThrow();

		// Assert
		assertThat(result)
			.isNotNull()
			.extracting(IncidentEntity::getJiraIssueKey, IncidentEntity::getPobIssueKey, IncidentEntity::getStatus)
			.containsExactly("JIR-001", "POB-001", SYNCHRONIZED);
	}

	@Test
	void findByPobIssueKeyNotFound() {

		// Arrange
		final var pobIssueKey = "non-existing";

		// Act
		final var result = repository.findByPobIssueKey(pobIssueKey);

		// Assert
		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void findByStatus() {

		// Act
		final var result = repository.findByStatus(SYNCHRONIZED);

		// Assert
		assertThat(result)
			.isNotNull()
			.extracting(IncidentEntity::getJiraIssueKey, IncidentEntity::getPobIssueKey, IncidentEntity::getStatus)
			.containsExactlyInAnyOrder(
				tuple("JIR-001", "POB-001", SYNCHRONIZED),
				tuple("JIR-009", "POB-009", SYNCHRONIZED),
				tuple("JIR-005", "POB-005", SYNCHRONIZED));
	}

	@Test
	void create() {

		// Arrange
		final var jiraIssueKey = "jiraIssueKey";
		final var pobIssueKey = "pobIssueKey";
		final var incident = IncidentEntity.create()
			.withJiraIssueKey(jiraIssueKey)
			.withPobIssueKey(pobIssueKey)
			.withStatus(SYNCHRONIZED);

		// Act
		final var result = repository.save(incident);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getJiraIssueKey()).isEqualTo(jiraIssueKey);
		assertThat(result.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(result.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(result.getModified()).isNull();
		assertThat(result.getLastSynchronizedJira()).isNull();
		assertThat(result.getLastSynchronizedPob()).isNull();
		assertThat(isValidUUID(result.getId())).isTrue();
	}

	private boolean isValidUUID(final String value) {
		try {
			UUID.fromString(String.valueOf(value));
		} catch (final Exception e) {
			return false;
		}

		return true;
	}
}
