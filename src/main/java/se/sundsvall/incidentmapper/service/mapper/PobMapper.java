package se.sundsvall.incidentmapper.service.mapper;

import static com.google.gson.FieldNamingPolicy.UPPER_CAMEL_CASE;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.compress.utils.FileNameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import com.chavaillaz.client.jira.domain.Attachment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import generated.se.sundsvall.pob.PobMemo;
import generated.se.sundsvall.pob.PobPayload;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;

public final class PobMapper {

	// Fields
	static final String PROBLEM = "Problem";
	static final String BINARY_DATA_TYPE = "BinaryData";
	static final String ORIGINAL_FILE_NAME = "OriginalFileName";
	static final String FILE_TYPE = "FileType";
	static final String ID = "Id";
	static final String CASE_TYPE = "Case";
	static final String FILE_DATA = "FileData";
	static final String RELATION = "Relation";
	static final String RESPONSIBLE = "Responsible";
	static final String RESPONSIBLE_GROUP = "ResponsibleGroup";

	// Field values
	private static final String DATA_URL_FORMAT = "data:%s;base64,%s";
	private static final String EXTENSION = ".html";

	// Json paths
	private static final String CASE_INTERNAL_NOTES_CUSTOM = "CaseInternalNotesCustom";
	private static final String JSON_PATH_DATA_DESCRIPTION = "$['Data']['Description']";
	private static final String JSON_PATH_MEMO_PROBLEM_MEMO = "$['Memo']['Problem']['Memo']";
	private static final String JSON_PATH_MEMO_CASE_INTERNAL_NOTES_CUSTOM_MEMO = "$['Memo']['CaseInternalNotesCustom']['Memo']";
	private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(UPPER_CAMEL_CASE).create();

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
