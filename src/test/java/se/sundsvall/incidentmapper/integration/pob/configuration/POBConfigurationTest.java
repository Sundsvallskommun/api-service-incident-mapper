package se.sundsvall.incidentmapper.integration.pob.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration.CLIENT_ID;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.incidentmapper.Application;

import feign.jackson.JacksonEncoder;

@SpringBootTest(classes = {Application.class, POBConfiguration.class, JacksonEncoder.class})
@ActiveProfiles("junit")
class POBConfigurationTest {

	@Autowired
	private POBConfiguration configuration;

	@Mock
	private POBProperties propertiesMock;

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Test
	void testErrorDecoder() {
		assertThat(configuration.errorDecoder()).isNotNull();
	}

	@Test
	void testEncoder() {
		assertThat(configuration.encoder()).isNotNull();
	}

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new POBConfiguration();

		when(propertiesMock.connectTimeout()).thenReturn(1);
		when(propertiesMock.readTimeout()).thenReturn(2);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (final var feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			final var customizer = configuration.feignBuilderCustomizer(propertiesMock);

			final var errorDecoderCaptor = ArgumentCaptor.forClass(JsonPathErrorDecoder.class);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(propertiesMock).connectTimeout();
			verify(propertiesMock).readTimeout();
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(1, 2);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			AssertionsForClassTypes.assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			AssertionsForClassTypes.assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}

}
