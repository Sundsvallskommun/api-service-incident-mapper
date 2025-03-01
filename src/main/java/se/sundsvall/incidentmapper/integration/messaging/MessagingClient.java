package se.sundsvall.incidentmapper.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.incidentmapper.integration.messaging.configuration.MessagingConfiguration.CLIENT_ID;

import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SlackRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.incidentmapper.integration.messaging.configuration.MessagingConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.messaging.url}", configuration = MessagingConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface MessagingClient {

	/**
	 * Send a single Slack message.
	 *
	 * @param  municipalityId the municipality ID.
	 * @param  slackRequest   containing the message to send
	 * @return                response containing id and delivery results for sent message
	 */
	@PostMapping(path = "/{municipalityId}/slack", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendSlackMessage(@PathVariable String municipalityId, @RequestBody SlackRequest slackRequest);
}
