package se.sundsvall.incidentmapper.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.dept44.test.annotation.resource.Load.ResourceType.JSON;

import com.chavaillaz.client.jira.domain.Attachment;
import generated.se.sundsvall.pob.PobPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.pob.model.Mail;

@ExtendWith(ResourceLoaderExtension.class)
class PobMapperTest {

	@Test
	void testToAttachmentPayload() {

		// Arrange
		final var attachment = new Attachment();
		attachment.setFilename("testFile.txt");
		final var base64String = "testBase64String";

		// Act
		final var result = PobMapper.toAttachmentPayload(attachment, base64String);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).containsEntry("FileData", "data:testFile.txt;base64,testBase64String");
		assertThat(result.getData()).containsEntry("OriginalFileName", "testFile.txt");
		assertThat(result.getData()).containsEntry("FileType", ".txt");
	}

	@Test
	void testToDescriptionPayload() {

		// Arrange
		final var entity = new IncidentEntity().withPobIssueKey("testId");
		final String jiraDescription = "testDescription";

		// Act
		final var result = PobMapper.toProblemPayload(entity, jiraDescription);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).containsEntry("Id", "testId");
		assertThat(result.getMemo().get("Problem").getMemo()).isEqualTo("testDescription");
	}

	@Test
	void testToResponsibleGroupPayload() {

		// Arrange
		final var responsibleUserGroupInPob = "The-user-group";
		final var pobIssueKey = "testId";

		// Act
		final var result = PobMapper.toResponsibleGroupPayload(pobIssueKey, responsibleUserGroupInPob);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).containsEntry("Id", "testId");
		assertThat(result.getData()).containsEntry("ResponsibleGroup", "The-user-group");
	}

	@Test
	void testToCaseInternalNotesCustomMemoPayload() {

		// Arrange
		final var entity = new IncidentEntity().withPobIssueKey("testId");
		final var comment = "testComment";

		// Act
		final var result = PobMapper.toCaseInternalNotesCustomMemoPayload(entity, comment);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getData()).containsEntry("Id", "testId");
		assertThat(result.getMemo().get("CaseInternalNotesCustom").getMemo()).isEqualTo("testComment");
	}

	@Test
	void toMail(@Load(value = "/PobMapperTest/pobPayloadMail.json", as = JSON) final PobPayload pobPayload) {

		// Act
		final var result = PobMapper.toMail(pobPayload);

		// Assert
		assertThat(result).isNotNull()
			.isEqualTo(Mail.create()
				.withAttachments(null)
				.withBody("Vi önskar avsluta det fasta abonnemanget. Vi önskar även att ni uppdaterar denna information på de platser där ni kan uppdatera")
				.withFrom("test.testsson@sundsvall.se")
				.withId("11831645")
				.withNumberOfAttachments(1)
				.withReplyTo("")
				.withSendDate("2024-04-10 09:50:12")
				.withSubject("Önskar övergå till enbart mobiltelefoni ")
				.withTo("test@sundsvall.se"));
	}

	@Test
	void toDescription(@Load(value = "/PobMapperTest/pobPayloadCase.json", as = JSON) final PobPayload pobPayload) {

		// Act
		final var result = PobMapper.toDescription(pobPayload);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo("This works!");
	}

	@Test
	void toProblemMemo(@Load(value = "/PobMapperTest/pobPayloadProblemMemo.json", as = JSON) final PobPayload pobPayload) {

		// Act
		final var result = PobMapper.toProblemMemo(pobPayload);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualToNormalizingWhitespace("""
				2024-04-10 10:02 Systemuser för POB WS  Admin

				Hej! Ville bara berätta att allt fungerar!


				Hälsningar

				Joe Doe
				Handläggare

				Avdelning 44
				Telefon: 060-00 00 00


				851 85 Sundsvall
				Besöksadress: Norrmalmsgatan 4
				Växel: 060-00 00 00
				www.sundsvall.se""");
	}

	@Test
	void toCaseInternalNotesCustomMemo(@Load(value = "/PobMapperTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) final PobPayload pobPayload) {

		// Act
		final var result = PobMapper.toCaseInternalNotesCustomMemo(pobPayload);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualToNormalizingWhitespace("""
				2024-05-08 14:09 System API Utvecklingsfabriken Utvecklingsfabriken
				Ev. förklaring till bytet av handläggargrupp

				___________________________________________________________


				2024-05-07 14:14 Joe Doe
				test interna anteckningar
				___________________________________________________________""");
	}
}
