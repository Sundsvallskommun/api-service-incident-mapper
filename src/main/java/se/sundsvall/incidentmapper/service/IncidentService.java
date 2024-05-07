package se.sundsvall.incidentmapper.service;

import static java.time.OffsetDateTime.now;
import static java.util.Arrays.asList;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.POB_INITIATED_EVENT;
import static se.sundsvall.incidentmapper.integration.db.model.enums.Status.SYNCHRONIZED;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.incidentmapper.api.model.IncidentRequest;
import se.sundsvall.incidentmapper.integration.db.IncidentRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;

@Service
@Transactional
public class IncidentService {

	private static final List<Status> OPEN_FOR_MODIFICATION_STATUS_LIST = asList(null, SYNCHRONIZED); // Status is only modifiable if current value is one of these.

	private final IncidentRepository incidentRepository;

	public IncidentService(IncidentRepository incidentRepository) {
		this.incidentRepository = incidentRepository;
	}

	public void handleIncidentRequest(IncidentRequest request) {

		final var issueKey = request.getIncidentKey();
		final var incidentEntity = incidentRepository.findByPobIssueKey(issueKey)
			.orElse(IncidentEntity.create().withPobIssueKey(issueKey));

		if (OPEN_FOR_MODIFICATION_STATUS_LIST.contains(incidentEntity.getStatus())) {
			incidentEntity.withStatus(POB_INITIATED_EVENT);
		}

		incidentRepository.save(incidentEntity.withPobIssueLastModified(now()));
	}

	public void pollJiraUpdates() {
		// Implement when JIRA API is ready
	}
}
