package se.sundsvall.incidentmapper.service.mapper;

import static com.google.gson.FieldNamingPolicy.UPPER_CAMEL_CASE;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.Optional;

import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

import generated.se.sundsvall.pob.PobMemo;
import generated.se.sundsvall.pob.PobPayload;

public final class PobMapper {

	public static final String DESCRIPTION = "description";

	public static final String BINARY_DATA = "BinaryData";

	public static final String FILE_DATA = "FileData";

	private static final String DATA_URL_FORMAT = "data:%s;base64,%s";

	private static final String RESPONSIBLE = "Responsible";

	private static final String RESPONSIBLE_GROUP = "ResponsibleGroup";

	private static final String IT_SUPPORT = "IT Support";

	private static final String EXTENSION = ".txt";

	private static final String ID = "id";

	private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(UPPER_CAMEL_CASE).create();

	private static final String JSON_PATH_DATA_DESCRIPTION = "$['Data']['Description']";

	private static final String JSON_PATH_MEMO_PROBLEM_MEMO = "$['Memo']['Problem']['Memo']";

	private static final String JSON_PATH_MEMO_CASE_INTERNAL_NOTES_CUSTOM_MEMO = "$['Memo']['CaseInternalNotesCustom']['Memo']";

	private static final String CASE_INTERNAL_NOTES_CUSTOM = "CaseInternalNotesCustom";


	private PobMapper() {
		// No instantiation allowed.
	}

	public static PobPayload toAttachmentPayload(final Attachment jiraAttachment, final String base64String) {
		return new PobPayload()
			.type(BINARY_DATA)
			.data(Map.of(FILE_DATA, DATA_URL_FORMAT.formatted(jiraAttachment.getFilename(), base64String)));
	}

	public static PobPayload toDescriptionPayload(final IncidentEntity entity, final String jiraDescription) {
		return new PobPayload().data(Map.of(ID, entity.getId(), DESCRIPTION, jiraDescription));
	}

	public static PobPayload toResponsibleGroupPayload(final IncidentEntity entity) {
		final Map<String, Object> data = Map.of(ID, entity.getId(), RESPONSIBLE, "", RESPONSIBLE_GROUP, IT_SUPPORT);
		return new PobPayload()
			.data(data);
	}

	public static PobPayload toCaseInternalNotesCustomMemoPayload(final IncidentEntity entity, final Comment comment) {
		final Map<String, Object> data = Map.of(ID, entity.getId());
		return new PobPayload()
			.data(data)
			.memo(Map.of(CASE_INTERNAL_NOTES_CUSTOM, new PobMemo()
				.extension(EXTENSION)
				.memo(comment.getBody())));
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
	 * @param pobPayload the payload to use
	 * @param jsonPath the path to verify existance of
	 * @return true if the key exist and has a non-null value.
	 */
	private static boolean jsonPathExists(final PobPayload pobPayload, final String jsonPath) {
		final var parsedJson = parse(GSON.toJson(pobPayload), defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS));
		return nonNull(parsedJson.read(jsonPath));
	}

	/**
	 * Extracts the value denoted by the provided jsonPath.
	 *
	 * @param pobPayload the payload to use
	 * @param jsonPath the path extract value from
	 * @param suppressExceptions whether to suppress exceptions for invalid paths, or not.
	 * @return the value that corresponds to the provided jsonPath.
	 */
	private static String extractValueFromJsonPath(final PobPayload pobPayload, final String jsonPath, final boolean suppressExceptions) {
		final var parsedJson = parse(GSON.toJson(pobPayload), suppressExceptions ? defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS) : defaultConfiguration());
		return parsedJson.read(jsonPath);
	}

}
