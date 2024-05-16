package se.sundsvall.incidentmapper.service.mapper;

import static com.google.gson.FieldNamingPolicy.UPPER_CAMEL_CASE;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static java.util.Objects.nonNull;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import generated.se.sundsvall.pob.PobPayload;

public final class PobMapper {

	private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(UPPER_CAMEL_CASE).create();
	private static final String JSON_PATH_DATA_DESCRIPTION = "$['Data']['Description']";
	private static final String JSON_PATH_MEMO_PROBLEM_MEMO = "$['Memo']['Problem']['Memo']";
	private static final String JSON_PATH_MEMO_CASEINTERNALNOTESCUSTOM_MEMO = "$['Memo']['CaseInternalNotesCustom']['Memo']";

	private PobMapper() {
		// No instantiation allowed.
	}

	public static String toDescription(PobPayload pobPayload) {
		return getValue(pobPayload, JSON_PATH_DATA_DESCRIPTION);
	}

	public static String toProblemMemo(PobPayload pobPayload) {
		return removeHTML(getValue(pobPayload, JSON_PATH_MEMO_PROBLEM_MEMO));
	}

	public static String toCaseInternalNotesCustomMemo(PobPayload pobPayload) {
		return removeHTML(getValue(pobPayload, JSON_PATH_MEMO_CASEINTERNALNOTESCUSTOM_MEMO));
	}

	private static String getValue(PobPayload pobPayload, String jsonPath) {
		return Optional.ofNullable(pobPayload)
			.filter(payLoad -> jsonPathExists(payLoad, jsonPath))
			.map(payLoad -> extractValueFromJsonPath(payLoad, jsonPath, false))
			.orElse(null);
	}

	private static String removeHTML(String source) {
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
	 * @return            true if the key exist and has a non null value.
	 */
	private static boolean jsonPathExists(PobPayload pobPayload, String jsonPath) {
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
	private static String extractValueFromJsonPath(PobPayload pobPayload, String jsonPath, boolean suppressExceptions) {
		final var parsedJson = parse(GSON.toJson(pobPayload), suppressExceptions ? defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS) : defaultConfiguration());
		return parsedJson.read(jsonPath);
	}
}
