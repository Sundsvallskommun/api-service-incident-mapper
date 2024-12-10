package se.sundsvall.incidentmapper.integration.pob.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailTest {

	@Test
	void testBean() {
		assertThat(Mail.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var attachments = List.of(new File("/"));
		final var body = "body";
		final var from = "from";
		final var id = "id";
		final var numberOfAttachments = 2;
		final var replyTo = "replyTo";
		final var sendDate = "sendDate";
		final var subject = "subject";
		final var to = "to";

		final var bean = Mail.create()
			.withAttachments(attachments)
			.withBody(body)
			.withFrom(from)
			.withId(id)
			.withNumberOfAttachments(numberOfAttachments)
			.withReplyTo(replyTo)
			.withSendDate(sendDate)
			.withSubject(subject)
			.withTo(to);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAttachments()).isEqualTo(attachments);
		assertThat(bean.getBody()).isEqualTo(body);
		assertThat(bean.getFrom()).isEqualTo(from);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getNumberOfAttachments()).isEqualTo(numberOfAttachments);
		assertThat(bean.getReplyTo()).isEqualTo(replyTo);
		assertThat(bean.getSendDate()).isEqualTo(sendDate);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getTo()).isEqualTo(to);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Mail.create()).hasAllNullFieldsOrPropertiesExcept("numberOfAttachments");
		assertThat(new Mail()).hasAllNullFieldsOrPropertiesExcept("numberOfAttachments");
	}
}
