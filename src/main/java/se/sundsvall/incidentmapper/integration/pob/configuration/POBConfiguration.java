package se.sundsvall.incidentmapper.integration.pob.configuration;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonEncoder;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder.JsonPathSetup;

@Import(FeignConfiguration.class)
public class POBConfiguration {

	public static final String CLIENT_ID = "pob";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final POBProperties pobProperties) {
		return FeignMultiCustomizer.create()
			.withRequestInterceptor(requestInterceptor(pobProperties))
			.withEncoder(encoder())
			.withErrorDecoder(errorDecoder())
			.withRequestTimeoutsInSeconds(pobProperties.connectTimeout(), pobProperties.readTimeout())
			.composeCustomizersToOne();
	}

	private Encoder encoder() {
		// Feign must be able to send null values.
		return new JacksonEncoder(new ObjectMapper().setDefaultPropertyInclusion(ALWAYS));
	}

	private ErrorDecoder errorDecoder() {
		// JsonPath below is constructed to only extract values from the attributes if they exist.
		// UserMessage and Message should never exist at the same time (according to API-spec).
		// 404:s should be thrown as 404:s and not 502:s
		return new JsonPathErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value()), new JsonPathSetup("concat($[?(@.UserMessage != null)].UserMessage, $[?(@.Message != null)].Message)", "concat($[?(@.InternalMessage != null)].InternalMessage)"));
	}

	private RequestInterceptor requestInterceptor(final POBProperties pobProperties) {
		return requestTemplate -> requestTemplate.header(AUTHORIZATION, pobProperties.apiKey());
	}
}
