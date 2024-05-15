package se.sundsvall.incidentmapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class ServiceUtilTest {

	@Test
	void toOffsetDateTime() {

		// Arrange
		final var dateTime = DateTime.parse("2010-06-30T01:20");
		// Act
		final var result = ServiceUtil.toOffsetDateTime(dateTime);
		// Assert
		assertThat(result).isEqualTo("2010-06-30T01:20+02:00");
	}

	@Test
	void inputStreamToBase64() {
		// Arrange
		final var inputStream = new java.io.ByteArrayInputStream("test".getBytes());
		// Act
		final var result = ServiceUtil.inputStreamToBase64(inputStream);
		// Assert
		assertThat(result).isEqualTo("dGVzdA==");
	}

	@Test
	void inputStreamToBase64_throwsException() throws IOException {
		// Arrange
		final var inputStream = mock(java.io.InputStream.class);
		when(inputStream.readAllBytes()).thenThrow(new IOException("test"));
		// Act & Assert
		assertThatThrownBy(() -> ServiceUtil.inputStreamToBase64(inputStream))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Failed to convert input stream to byte array: test");


	}

	@Test
	void stripHtmlTags() {

		// Arrange
		final var html = "<html><head></head><body style=\\\"font-family:Segoe UI;font-size:9pt;\\\"><div id='memo1360031273'><div><div style=\\\"font-family:Segoe UI;font-size:9pt;\\\">2024-05-08 14:09 System API Utvecklingsfabriken Utvecklingsfabriken</div><div style=\\\"font-family:Segoe UI;font-size:9pt;\\\">Ev. förklaring till bytet av handläggargrupp</div><div><div style=\\\"font-family:Segoe UI;font-size:9pt;\\\"><br>___________________________________________________________<br></div></div></div></div><div id='memo1360031159'><p class=\\\"pobg6_delimiter\\\" style=\\\"margin:0 0 0 0;\\\">2024-05-07 14:14 Test Testsson Testvall</p><p style=\\\"margin:0 0 0 0;\\\">test interna anteckningar</p><p class=\\\"pobg6_separator\\\" style=\\\"margin:0 0 0 0;\\\">___________________________________________________________</p></div></body></html>";
		final var expected = """
			2024-05-08 14:09 System API Utvecklingsfabriken Utvecklingsfabriken
			Ev. förklaring till bytet av handläggargrupp
			___________________________________________________________
			2024-05-07 14:14 Test Testsson Testvall
			test interna anteckningar
			___________________________________________________________""";

		// Act
		final var result = ServiceUtil.stripHtmlTags(html);

		// Assert
		assertThat(result).isEqualTo(expected);
	}


}
