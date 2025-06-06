package se.sundsvall.incidentmapper.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.incidentmapper.integration.db.model.IncidentEntity;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;

@CircuitBreaker(name = "incidentRepository")
public interface IncidentRepository extends JpaRepository<IncidentEntity, String> {

	/**
	 * Find by POB issue key.
	 *
	 * @param  municipalityId the municipality ID.
	 * @param  pobIssueKey    the POB issue key
	 * @return                an Optional IncidentEntity.
	 */
	Optional<IncidentEntity> findByMunicipalityIdAndPobIssueKey(String municipalityId, String jiraIssueKey);

	/**
	 * Find all by status.
	 *
	 * @param  status the status to fetch incidents by
	 * @return        a List of IncidentEntities.
	 */
	List<IncidentEntity> findByStatus(Status status);
}
