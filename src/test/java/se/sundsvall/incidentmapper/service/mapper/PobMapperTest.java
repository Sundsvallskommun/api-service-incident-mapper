package se.sundsvall.incidentmapper.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.dept44.test.annotation.resource.Load.ResourceType.JSON;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import generated.se.sundsvall.pob.PobPayload;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith(ResourceLoaderExtension.class)
class PobMapperTest {

	@Test
	void toDescription(@Load(value = "/PobMapperTest/pobPayloadCase.json", as = JSON) PobPayload pobPayload) {

		// Act
		final var result = PobMapper.toDescription(pobPayload);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEqualTo("This works!");
	}

	@Test
	void toProblemMemo(@Load(value = "/PobMapperTest/pobPayloadProblemMemo.json", as = JSON) PobPayload pobPayload) {

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
	void toCaseInternalNotesCustomMemo(@Load(value = "/PobMapperTest/pobPayloadCaseInternalNotesCustomMemo.json", as = JSON) PobPayload pobPayload) {

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
