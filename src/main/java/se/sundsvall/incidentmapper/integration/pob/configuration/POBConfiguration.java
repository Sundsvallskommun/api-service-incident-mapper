package se.sundsvall.incidentmapper.integration.pob.configuration;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder.JsonPathSetup;

import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonEncoder;

@Import(FeignConfiguration.class)
public class POBConfiguration {

	public static final String CLIENT_ID = "pob";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final POBProperties pobProperties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(errorDecoder())
			.withRequestTimeoutsInSeconds(pobProperties.connectTimeout(), pobProperties.readTimeout())
			.composeCustomizersToOne();
	}

	@Bean
	Encoder encoder() {
		return new JacksonEncoder(new ObjectMapper()
			.setDefaultPropertyInclusion(ALWAYS)); // Feign must be able to send null values.
	}

	@Bean
	ErrorDecoder errorDecoder() {
		// JsonPath below is constructed to only extract values from the attributes if they exist.
		// UserMessage and Message should never exist at the same time (according to API-spec).
		// 404:s should be thrown as 404:s and not 502:s
		return new JsonPathErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value()), new JsonPathSetup("concat($[?(@.UserMessage != null)].UserMessage, $[?(@.Message != null)].Message)", "concat($[?(@.InternalMessage != null)].InternalMessage)"));
	}
}
