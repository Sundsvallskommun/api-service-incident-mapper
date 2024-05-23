package se.sundsvall.incidentmapper.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.messaging.SlackRequest;
import se.sundsvall.incidentmapper.integration.messaging.MessagingClient;
import se.sundsvall.incidentmapper.integration.messaging.configuration.MessagingProperties;

@ExtendWith(MockitoExtension.class)
class SlackServiceTest {

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private MessagingProperties messagingPropertiesMock;

	@InjectMocks
	private SlackService slackService;

	@Test
	void testSlack() {

		// Arrange
		final var token = "token";
		final var channel = "channel";
		final var message = "This is a testmessage";

		when(messagingPropertiesMock.channel()).thenReturn(channel);
		when(messagingPropertiesMock.token()).thenReturn(token);

		// Act
		slackService.sendToSlack(message);

		// Assert
		verify(messagingPropertiesMock).channel();
		verify(messagingPropertiesMock).token();
		verify(messagingClientMock).sendSlack(new SlackRequest()
			.channel(channel)
			.token(token)
			.message(message));
	}
}
