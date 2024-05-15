package se.sundsvall.incidentmapper.service;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.zalando.problem.Problem;

public class ServiceUtil {

	public static OffsetDateTime toOffsetDateTime(final DateTime jodaDateTime) {
		return Optional.ofNullable(jodaDateTime)
			.map(joda -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(jodaDateTime.getMillis()), ZoneId.of(jodaDateTime.getZone().getID())))
			.orElse(null);
	}


	public static String inputStreamToBase64(final InputStream inputStream) {
		try {
			return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to convert input stream to byte array: " + e.getMessage());
		}
	}

	public static String stripHtmlTags(final String html) {
		return Arrays.stream(html.replace("><", ">\n<")
				.replaceAll("<[^>]*>", "").split("\n"))
			.filter(line -> !line.trim().isEmpty())
			.collect(Collectors.joining("\n"));
	}

}
