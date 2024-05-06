package se.sundsvall.incidentmapper.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.sundsvall.incidentmapper.integration.db.model.enums.Status;

class IncidentEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(IncidentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var created = now();
		final var id = "id";
		final var jiraIssueKey = "jiraIssueKey";
		final var jiraIssueLastModified = now();
		final var modified = now();
		final var pobIssueKey = "pobIssueKey";
		final var pobIssueLastModified = now();
		final var status = Status.CLOSED;

		final var bean = IncidentEntity.create()
			.withCreated(created)
			.withId(id)
			.withJiraIssueKey(jiraIssueKey)
			.withJiraIssueLastModified(jiraIssueLastModified)
			.withModified(modified)
			.withPobIssueKey(pobIssueKey)
			.withPobIssueLastModified(pobIssueLastModified)
			.withStatus(status);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getJiraIssueKey()).isEqualTo(jiraIssueKey);
		assertThat(bean.getJiraIssueLastModified()).isEqualTo(jiraIssueLastModified);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getPobIssueKey()).isEqualTo(pobIssueKey);
		assertThat(bean.getPobIssueLastModified()).isEqualTo(pobIssueLastModified);
		assertThat(bean.getStatus()).isEqualTo(status);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(IncidentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new IncidentEntity()).hasAllNullFieldsOrProperties();
	}
}
