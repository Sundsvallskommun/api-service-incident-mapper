package se.sundsvall.incidentmapper.integration.pob;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration.CLIENT_ID;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration;

import generated.se.sundsvall.pob.PobPayload;
import generated.se.sundsvall.pob.PobPayloadWithTriggerResults;
import generated.se.sundsvall.pob.SuspensionInfo;


// TODO: These are the endpoints used in SupportCenter. They should be updated to match the endpoints
// TODO: we will use in IncidentMapper. When we get documentation from POB, we should update these endpoints.
@FeignClient(name = CLIENT_ID, url = "${integration.pob.url}", configuration = POBConfiguration.class)
public interface POBClient {

	/**
	 * Updates an existing case in POB.
	 *
	 * @param pobKey the key to use for authorization
	 * @param payload the object with the updated case-attributes.
	 */
	@PostMapping(path = "cases", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void updateCase(@RequestHeader(AUTHORIZATION) String pobKey, @RequestBody PobPayload payload);

	/**
	 * Creates a case in POB.
	 *
	 * @param pobKey the key to use for authorization
	 * @param payload payload the object with the item to create.
	 * @return a list of payload trigger results
	 */
	@PutMapping(path = "cases", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	List<PobPayloadWithTriggerResults> createCase(@RequestHeader(AUTHORIZATION) String pobKey, @RequestBody PobPayload payload);


	/**
	 * Get an existing case in POB.
	 *
	 * @param pobKey the key to use for authorization
	 * @param caseId the ID of the case.
	 * @return The case
	 */
	@GetMapping(path = "cases/{caseId}", produces = APPLICATION_JSON_VALUE)
	PobPayload getCase(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("caseId") String caseId);

	/**
	 * Returns a list of all available case-categories.
	 *
	 * @param pobKey the key to use for authorization
	 * @return a list of available case-categories (max 10000)
	 */
	@Cacheable("case-categories")
	@GetMapping(path = "casecategories?Fields=Id&limit=10000", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getCaseCategories(@RequestHeader(AUTHORIZATION) String pobKey);

	/**
	 * Returns a list of all available closure-codes.
	 *
	 * @param pobKey the key to use for authorization
	 * @return a list of available closure-codes (max 10000)
	 */
	@Cacheable("closure-codes")
	@GetMapping(path = "closurecodes?Fields=Id&limit=10000", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getClosureCodes(@RequestHeader(AUTHORIZATION) String pobKey);

	/**
	 * Returns a list of configuration-items by serialNumber. The returned objects only contains the ID-attribute.
	 *
	 * @param pobKey the key to use for authorization
	 * @param serialNumber the serial number to filter the results on
	 * @return a list of configuration-items
	 */
	@GetMapping(path = "configurationitems?Filter=SerialNumber={serialNumber}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getConfigurationItemsBySerialNumber(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("serialNumber") String serialNumber);

	/**
	 * Returns a list containing an item by id.
	 *
	 * @param pobKey the key to use for authorization
	 * @param id id of an item
	 * @return a list containing an item
	 */
	@GetMapping(path = "items?Filter=Id={id}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getItemsById(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("id") String id);

	/**
	 * Returns a list of items by model-name. The returned objects only contains the ID-attribute.
	 *
	 * @param pobKey the key to use for authorization
	 * @param modelName the item model name to filter response on
	 * @return a list of configuration-items
	 */
	@GetMapping(path = "items?Filter=IdOption={modelName}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getItemsByModelName(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("modelName") String modelName);

	/**
	 * Creates an item
	 *
	 * @param pobKey the key to use for authorization
	 * @param payload the object with the updated configuration attributes
	 * @return list of pob-payloads with triggered results
	 */
	@PutMapping(path = "items", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	List<PobPayloadWithTriggerResults> createItem(@RequestHeader(AUTHORIZATION) String pobKey, @RequestBody PobPayload payload);

	/**
	 * Updates a configuration-item
	 *
	 * @param pobKey the key to use for authorization
	 * @param payload the object with configuration-items to create
	 * @return list of pob-payloads with triggered results
	 */
	@PutMapping(path = "configurationitems", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	List<PobPayloadWithTriggerResults> createConfigurationItem(@RequestHeader(AUTHORIZATION) String pobKey, @RequestBody PobPayload payload);

	/**
	 * Updates a configuration-item
	 *
	 * @param pobKey the key to use for authorization
	 * @param payload the object with the updated configuration attributes
	 */
	@PostMapping(path = "configurationitems", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void updateConfigurationItem(@RequestHeader(AUTHORIZATION) String pobKey, @RequestBody PobPayload payload);

	/**
	 * Suspend an existing case in POB.
	 *
	 * @param pobKey the key to use for authorization
	 * @param caseId the ID of the case
	 * @param payload object with suspension information
	 */
	@PostMapping(path = "cases/{caseId}/suspension", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void suspendCase(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("caseId") String caseId, @RequestBody SuspensionInfo payload);

	/**
	 * Get suspension information for a suspended case in POB, if case is not suspended a 404 will be thrown.
	 *
	 * @param pobKey the key to use for authorization
	 * @param caseId the ID of the case
	 * @return Information about the suspension
	 */
	@GetMapping(path = "cases/{caseId}/suspension", produces = APPLICATION_JSON_VALUE)
	SuspensionInfo getSuspension(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("caseId") String caseId);

	/**
	 * Unsuspend a suspended case in POB.
	 *
	 * @param pobKey the key to use for authorization
	 * @param caseId the ID of the case
	 */
	@DeleteMapping(path = "cases/{caseId}/suspension", produces = APPLICATION_JSON_VALUE)
	Void deleteSuspension(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable("caseId") String caseId);

}
