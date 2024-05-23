package se.sundsvall.incidentmapper.service;

import org.springframework.stereotype.Service;

import generated.se.sundsvall.messaging.SlackRequest;
import se.sundsvall.incidentmapper.integration.messaging.MessagingClient;
import se.sundsvall.incidentmapper.integration.messaging.configuration.MessagingProperties;

@Service
public class SlackService {

	private final MessagingClient messagingClient;
	private final MessagingProperties messagingProperties;

	public SlackService(MessagingClient messagingClient, MessagingProperties messagingProperties) {
		this.messagingClient = messagingClient;
		this.messagingProperties = messagingProperties;
	}

	public void sendToSlack(String message) {
		messagingClient.sendSlack(new SlackRequest()
			.channel(messagingProperties.channel())
			.token(messagingProperties.token())
			.message(message));
	}
}
