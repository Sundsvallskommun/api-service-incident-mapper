package se.sundsvall.incidentmapper.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class IncidentRequestTest {

	@Test
	void testBean() {
		assertThat(IncidentRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var incidentKey = "incidentKey";

		final var bean = IncidentRequest.create()
			.withIncidentKey(incidentKey);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getIncidentKey()).isEqualTo(incidentKey);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(IncidentRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new IncidentRequest()).hasAllNullFieldsOrProperties();
	}
}
