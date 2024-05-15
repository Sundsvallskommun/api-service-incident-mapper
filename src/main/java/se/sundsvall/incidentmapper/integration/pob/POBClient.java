package se.sundsvall.incidentmapper.integration.pob;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration.CLIENT_ID;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration;

import generated.se.sundsvall.pob.PobPayload;
import generated.se.sundsvall.pob.PobPayloadWithTriggerResults;
import generated.se.sundsvall.pob.SuspensionInfo;

// TODO: These are the endpoints used in SupportCenter. They should be updated to match the endpoints
// TODO: we will use in IncidentMapper. When we get documentation from POB, we should update these endpoints.
@FeignClient(name = CLIENT_ID, url = "${integration.pob.url}", configuration = POBConfiguration.class, dismiss404 = true)
public interface POBClient {

	/**
	 * Updates an existing case in POB.
	 *
	 * @param payload the object with the updated case-attributes.
	 */
	@PostMapping(path = "case", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void updateCase(@RequestBody PobPayload payload);

	/**
	 * Creates a case in POB.
	 *
	 * @param payload payload the object with the item to create.
	 * @return a list of payload trigger results
	 */
	@PutMapping(path = "case", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	List<PobPayloadWithTriggerResults> createCase(@RequestBody PobPayload payload);

	/**
	 * Get an existing case in POB.
	 *
	 * @param caseId the ID of the case.
	 * @return The pobPayload
	 */
	@GetMapping(path = "case/{caseId}", produces = APPLICATION_JSON_VALUE)
	PobPayload getCase(@PathVariable("caseId") String caseId);

	/**
	 * Get problem memo (i.e. error description) for an existing case in POB.
	 *
	 * @param  caseId the ID of the case.
	 * @return        The pobPayload
	 */
	@GetMapping(path = "case/{caseId}/memo?type=Problem&scope=all", produces = APPLICATION_JSON_VALUE)
	Optional<PobPayload> getProblemMemo(@PathVariable("caseId") String caseId);

	/**
	 * Get case internal notes (i.e. internal notes) for an existing case in POB.
	 *
	 * @param  caseId the ID of the case.
	 * @return        The pobPayload
	 */
	@GetMapping(path = "case/{caseId}/memo?type=CaseInternalNotesCustom&scope=all", produces = APPLICATION_JSON_VALUE)
	Optional<PobPayload> getCaseInternalNotesCustom(@PathVariable("caseId") String caseId);

	/**
	 * Returns a list of all available case-categories.
	 *
	 * @return a list of available case-categories (max 10000)
	 */
	@Cacheable("case-categories")
	@GetMapping(path = "casecategories?Fields=Id&limit=10000", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getCaseCategories();

	/**
	 * Returns a memo by caseId, type and scope.
	 *
	 * @return a payload with the memo
	 */
	@GetMapping(path = "case/{caseId}/memo?type={type}&scope={scope}", produces = APPLICATION_JSON_VALUE)
	PobPayload getMemo(@PathVariable("caseId") String caseId, @PathVariable("type") String type, @PathVariable("scope") String scope);

	/**
	 * Returns a list of all attachments for a case.
	 *
	 * @return a payload with the attachments
	 */
	@GetMapping(path = "case/{caseId}/attachments", produces = APPLICATION_JSON_VALUE)
	PobPayload getAttachments(@PathVariable("caseId") String caseId);


	@PutMapping(path = "case/{caseId}/attachments", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void createAttachment(@PathVariable("caseId") String caseId, @RequestBody PobPayload payload);

	/**
	 * Returns a list of all available closure-codes.
	 *
	 * @return a list of available closure-codes (max 10000)
	 */
	@Cacheable("closure-codes")
	@GetMapping(path = "closurecodes?Fields=Id&limit=10000", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getClosureCodes();

	/**
	 * Returns a list of configuration-items by serialNumber. The returned objects only contains the ID-attribute.
	 *
	 * @param pobKey the key to use for authorization
	 * @param serialNumber the serial number to filter the results on
	 * @return a list of configuration-items
	 */
	@GetMapping(path = "configurationitems?Filter=SerialNumber={serialNumber}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getConfigurationItemsBySerialNumber(@PathVariable("serialNumber") String serialNumber);

	/**
	 * Returns a list containing an item by id.
	 *
	 * @param id id of an item
	 * @return a list containing an item
	 */
	@GetMapping(path = "items?Filter=Id={id}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getItemsById(@PathVariable("id") String id);

	/**
	 * Returns a list of items by model-name. The returned objects only contains the ID-attribute.
	 *
	 * @param modelName the item model name to filter response on
	 * @return a list of configuration-items
	 */
	@GetMapping(path = "items?Filter=IdOption={modelName}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getItemsByModelName(@PathVariable("modelName") String modelName);

	/**
	 * Creates an item
	 *
	 * @param payload the object with the updated configuration attributes
	 * @return list of pob-payloads with triggered results
	 */
	@PutMapping(path = "items", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	List<PobPayloadWithTriggerResults> createItem(@RequestBody PobPayload payload);

	/**
	 * Updates a configuration-item
	 *
	 * @param payload the object with configuration-items to create
	 * @return list of pob-payloads with triggered results
	 */
	@PutMapping(path = "configurationitems", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	List<PobPayloadWithTriggerResults> createConfigurationItem(@RequestBody PobPayload payload);

	/**
	 * Updates a configuration-item
	 *
	 * @param payload the object with the updated configuration attributes
	 */
	@PostMapping(path = "configurationitems", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void updateConfigurationItem(@RequestBody PobPayload payload);

	/**
	 * Suspend an existing case in POB.
	 *
	 * @param caseId the ID of the case
	 * @param payload object with suspension information
	 */
	@PostMapping(path = "case/{caseId}/suspension", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void suspendCase(@PathVariable("caseId") String caseId, @RequestBody SuspensionInfo payload);

	/**
	 * Get suspension information for a suspended case in POB, if case is not suspended a 404 will be thrown.
	 *
	 * @param caseId the ID of the case
	 * @return Information about the suspension
	 */
	@GetMapping(path = "case/{caseId}/suspension", produces = APPLICATION_JSON_VALUE)
	SuspensionInfo getSuspension(@PathVariable("caseId") String caseId);

	/**
	 * Unsuspend a suspended case in POB.
	 *
	 * @param caseId the ID of the case
	 */
	@DeleteMapping(path = "case/{caseId}/suspension", produces = APPLICATION_JSON_VALUE)
	Void deleteSuspension(@PathVariable("caseId") String caseId);


	@GetMapping(path = "case/{caseId}/attachments/{attachmentId}", produces = APPLICATION_JSON_VALUE)
	InputStream getAttachment(@PathVariable("caseId") String caseId, @PathVariable("attachmentId") String attachmentId);

}
