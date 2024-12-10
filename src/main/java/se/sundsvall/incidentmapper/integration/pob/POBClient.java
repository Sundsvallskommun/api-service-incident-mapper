package se.sundsvall.incidentmapper.integration.pob;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration.CLIENT_ID;

import generated.se.sundsvall.pob.PobPayload;
import java.util.List;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.incidentmapper.integration.pob.configuration.POBConfiguration;

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
	 * Get an existing case in POB.
	 *
	 * @param  caseId the ID of the case.
	 * @return        The pobPayload
	 */
	@GetMapping(path = "case/{caseId}", produces = APPLICATION_JSON_VALUE)
	Optional<PobPayload> getCase(@PathVariable("caseId") String caseId);

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
	 * Returns a list of all attachments for a case in POB.
	 *
	 * @return a payload with the attachments
	 */
	@GetMapping(path = "case/{caseId}/attachments", produces = APPLICATION_JSON_VALUE)
	Optional<PobPayload> getAttachments(@PathVariable("caseId") String caseId);

	/**
	 * Returns an attachment for a case in POB.
	 *
	 * @param  caseId       the ID of the case.
	 * @param  attachmentId the ID of the attachment.
	 * @return              the attachment
	 */
	@GetMapping(path = "case/{caseId}/attachments/{attachmentId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<InputStreamResource> getAttachment(@PathVariable("caseId") String caseId, @PathVariable("attachmentId") String attachmentId);

	/**
	 * Creates an attachment for a case in POB.
	 *
	 * @param caseId  the ID of the case.
	 * @param payload the object with the attachment to create.
	 */
	@PutMapping(path = "case/{caseId}/attachments", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	Void createAttachment(@PathVariable("caseId") String caseId, @RequestBody PobPayload payload);

	/**
	 * Returns a list of all received mails for a case (mailStatus=3).
	 *
	 * Please note: The payload will only contain an "Id". Use the readMail-method to fetch the content.
	 *
	 * @param  caseId the ID of the case.
	 * @return        a payload with the mail information.
	 */
	@GetMapping(path = "case/{caseId}/mails?Filter=MailStatus=3&Fields=Id", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getReceivedMailIds(@PathVariable("caseId") String caseId);

	/**
	 * Fetch a mail by Id.
	 *
	 * @param  mailId the ID of the mail.
	 * @return        a payload with the mail data.
	 */
	@GetMapping(path = "mail/{mailId}", produces = APPLICATION_JSON_VALUE)
	Optional<PobPayload> getMail(@PathVariable("mailId") String mailId);
}
