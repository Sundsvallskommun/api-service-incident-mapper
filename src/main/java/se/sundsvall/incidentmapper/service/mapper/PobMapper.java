package se.sundsvall.incidentmapper.service.mapper;

import static com.google.gson.FieldNamingPolicy.UPPER_CAMEL_CASE;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.math.NumberUtils.toInt;

import com.chavaillaz.client.jira.domain.Attachment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import generated.se.sundsvall.pob.PobMemo;
import generated.se.sundsvall.pob.PobPayload;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.pob.model.Mail;

public final class PobMapper {

	// Fields
	private static final String PROBLEM = "Problem";
	private static final String BINARY_DATA_TYPE = "BinaryData";
	private static final String ORIGINAL_FILE_NAME = "OriginalFileName";
	private static final String FILE_TYPE = "FileType";
	private static final String ID = "Id";
	private static final String CASE_TYPE = "Case";
	private static final String FILE_DATA = "FileData";
	private static final String RESPONSIBLE = "Responsible";
	private static final String RESPONSIBLE_GROUP = "ResponsibleGroup";
	private static final String CASE_INTERNAL_NOTES_CUSTOM = "CaseInternalNotesCustom";
	private static final String NUMBER_OF_ATTACHMENTS = "NumberOfAttachments";

	private static final String MAIL_FROM = "MailFrom";
	private static final String MAIL_REPLY_TO = "ReplyTo";
	private static final String MAIL_SEND_DATE = "SendDate";
	private static final String MAIL_SUBJECT = "Subject";
	private static final String MAIL_TO = "MailTo";

	// Field values
	private static final String DATA_URL_FORMAT = "data:%s;base64,%s";
	private static final String EXTENSION = ".html";

	// Json paths
	private static final String JSON_PATH_DATA_DESCRIPTION = "$['Data']['Description']";
	private static final String JSON_PATH_MEMO_PROBLEM_MEMO = "$['Memo']['Problem']['Memo']";
	private static final String JSON_PATH_MEMO_MAIL_MEMO = "$['Memo']['Mail']['Memo']";
	private static final String JSON_PATH_MEMO_CASE_INTERNAL_NOTES_CUSTOM_MEMO = "$['Memo']['CaseInternalNotesCustom']['Memo']";
	private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(UPPER_CAMEL_CASE).create();

	// Format templates
	private static final String FORMATTED_MAIL_TEMPLATE = """
		_____________________________________________________
		To: %s
		From: %s
		Subject: %s
		Date: %s
		Attachments: %s
		_____________________________________________________

		%s
		""";

	private PobMapper() {
		// No instantiation allowed.
	}

	public static PobPayload toAttachmentPayload(final Attachment jiraAttachment, final String base64String) {
		return new PobPayload()
			.type(BINARY_DATA_TYPE)
			.data(Map.of(
				FILE_TYPE, "." + FileNameUtils.getExtension(jiraAttachment.getFilename()),
				ORIGINAL_FILE_NAME, jiraAttachment.getFilename(),
				FILE_DATA, DATA_URL_FORMAT.formatted(jiraAttachment.getFilename(), base64String)));
	}

	public static PobPayload toProblemPayload(final IncidentEntity entity, final String jiraDescription) {
		final Map<String, Object> data = Map.of(ID, entity.getPobIssueKey());
		return new PobPayload()
			.type(CASE_TYPE)
			.data(data)
			.memo(Map.of(PROBLEM, new PobMemo()
				.extension(EXTENSION)
				.handleSeparators(true)
				.memo(jiraDescription)));
	}

	public static PobPayload toResponsibleGroupPayload(final String pobIssueKey, String responsibleUserGroupInPob) {
		final var data = new HashMap<String, Object>();
		data.put(ID, pobIssueKey);
		data.put(RESPONSIBLE, null);
		data.put(RESPONSIBLE_GROUP, responsibleUserGroupInPob);
		return new PobPayload()
			.type(CASE_TYPE)
			.data(data);
	}

	public static PobPayload toCaseInternalNotesCustomMemoPayload(final IncidentEntity entity, final String comment) {
		final Map<String, Object> data = Map.of(ID, entity.getPobIssueKey());
		return new PobPayload()
			.type(CASE_TYPE)
			.data(data)
			.memo(Map.of(CASE_INTERNAL_NOTES_CUSTOM, new PobMemo()
				.extension(EXTENSION)
				.handleSeparators(true)
				.memo(comment)));
	}

	public static Mail toMail(final PobPayload pobPayload) {
		return Mail.create()
			.withBody(removeHTML(getValue(pobPayload, JSON_PATH_MEMO_MAIL_MEMO)))
			.withFrom((String) pobPayload.getData().get(MAIL_FROM))
			.withId((String) pobPayload.getData().get(ID))
			.withNumberOfAttachments(toInt((String) pobPayload.getData().get(NUMBER_OF_ATTACHMENTS), 0))
			.withReplyTo((String) pobPayload.getData().get(MAIL_REPLY_TO))
			.withSendDate((String) pobPayload.getData().get(MAIL_SEND_DATE))
			.withSubject((String) pobPayload.getData().get(MAIL_SUBJECT))
			.withTo((String) pobPayload.getData().get(MAIL_TO));
	}

	public static String toFormattedMail(final Mail mail) {
		return Optional.ofNullable(mail)
			.map(m -> FORMATTED_MAIL_TEMPLATE.formatted(
				m.getTo(),
				m.getFrom(),
				m.getSubject(),
				m.getSendDate(),
				Optional.ofNullable(m.getAttachments()).orElse(emptyList()).stream()
					.map(File::getName)
					.collect(joining(", ")),
				m.getBody()))
			.orElse(null);
	}

	public static String toDescription(final PobPayload pobPayload) {
		return getValue(pobPayload, JSON_PATH_DATA_DESCRIPTION);
	}

	public static String toProblemMemo(final PobPayload pobPayload) {
		return removeHTML(getValue(pobPayload, JSON_PATH_MEMO_PROBLEM_MEMO));
	}

	public static String toCaseInternalNotesCustomMemo(final PobPayload pobPayload) {
		return removeHTML(getValue(pobPayload, JSON_PATH_MEMO_CASE_INTERNAL_NOTES_CUSTOM_MEMO));
	}

	private static String getValue(final PobPayload pobPayload, final String jsonPath) {
		return Optional.ofNullable(pobPayload)
			.filter(payLoad -> jsonPathExists(payLoad, jsonPath))
			.map(payLoad -> extractValueFromJsonPath(payLoad, jsonPath, false))
			.orElse(null);
	}

	private static String removeHTML(final String source) {
		return Optional.ofNullable(source)
			.map(string -> {
				final var outputSettings = new Document.OutputSettings()
					.prettyPrint(false);

				final var jsoupDoc = Jsoup.parse(source);
				jsoupDoc.outputSettings(outputSettings);
				jsoupDoc.select("br").before("\\n");
				jsoupDoc.select("p").before("\\n");
				jsoupDoc.select("div").before("\\n");

				final var parsedText = jsoupDoc.html().replace("\\n", "\n");
				return Jsoup.clean(parsedText, "", Safelist.none(), outputSettings).trim();
			})
			.orElse(null);
	}

	/**
	 * Checks if the key denoted by the provided jsonPath exists and has a value that is not null.
	 *
	 * @param  pobPayload the payload to use
	 * @param  jsonPath   the path to verify existance of
	 * @return            true if the key exist and has a non-null value.
	 */
	private static boolean jsonPathExists(final PobPayload pobPayload, final String jsonPath) {
		final var parsedJson = parse(GSON.toJson(pobPayload), defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS));
		return nonNull(parsedJson.read(jsonPath));
	}

	/**
	 * Extracts the value denoted by the provided jsonPath.
	 *
	 * @param  pobPayload         the payload to use
	 * @param  jsonPath           the path extract value from
	 * @param  suppressExceptions whether to suppress exceptions for invalid paths, or not.
	 * @return                    the value that corresponds to the provided jsonPath.
	 */
	private static String extractValueFromJsonPath(final PobPayload pobPayload, final String jsonPath, final boolean suppressExceptions) {
		final var parsedJson = parse(GSON.toJson(pobPayload), suppressExceptions ? defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS) : defaultConfiguration());
		return parsedJson.read(jsonPath);
	}
}
