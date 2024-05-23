package se.sundsvall.incidentmapper.integration.db.model;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import se.sundsvall.incidentmapper.integration.db.listener.IncidentEntityListener;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;

@Entity
@Table(
	name = "incident",
	uniqueConstraints = {
		@UniqueConstraint(name = "incident_unique_pob_issue_key_constraint", columnNames = { "pob_issue_key" }),
		@UniqueConstraint(name = "incident_unique_jira_issue_key_constraint", columnNames = { "jira_issue_key" })
	},
	indexes = {
		@Index(name = "ix_pob_issue_key", columnList = "pob_issue_key"),
		@Index(name = "ix_jira_issue_key", columnList = "jira_issue_key"),
		@Index(name = "ix_status", columnList = "status")
	})
@EntityListeners(IncidentEntityListener.class)
public class IncidentEntity implements Serializable {

	private static final long serialVersionUID = 2395569293200765514L;

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "pob_issue_key")
	private String pobIssueKey;

	@Column(name = "jira_issue_key")
	private String jiraIssueKey;

	@Column(name = "status")
	private Status status;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "last_synchronized_jira")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime lastSynchronizedJira;

	@Column(name = "last_synchronized_pob")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime lastSynchronizedPob;

	public static IncidentEntity create() {
		return new IncidentEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public IncidentEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getPobIssueKey() {
		return pobIssueKey;
	}

	public void setPobIssueKey(String pobIssueKey) {
		this.pobIssueKey = pobIssueKey;
	}

	public IncidentEntity withPobIssueKey(String pobIssueKey) {
		this.pobIssueKey = pobIssueKey;
		return this;
	}

	public String getJiraIssueKey() {
		return jiraIssueKey;
	}

	public void setJiraIssueKey(String jiraIssueKey) {
		this.jiraIssueKey = jiraIssueKey;
	}

	public IncidentEntity withJiraIssueKey(String jiraIssueKey) {
		this.jiraIssueKey = jiraIssueKey;
		return this;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public IncidentEntity withStatus(Status status) {
		this.status = status;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public IncidentEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public IncidentEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public OffsetDateTime getLastSynchronizedJira() {
		return lastSynchronizedJira;
	}

	public void setLastSynchronizedJira(OffsetDateTime lastSynchronizedJira) {
		this.lastSynchronizedJira = lastSynchronizedJira;
	}

	public IncidentEntity withLastSynchronizedJira(OffsetDateTime lastSynchronizedJira) {
		this.lastSynchronizedJira = lastSynchronizedJira;
		return this;
	}

	public OffsetDateTime getLastSynchronizedPob() {
		return lastSynchronizedPob;
	}

	public void setLastSynchronizedPob(OffsetDateTime lastSynchronizedPob) {
		this.lastSynchronizedPob = lastSynchronizedPob;
	}

	public IncidentEntity withLastSynchronizedPob(OffsetDateTime lastSynchronizedPob) {
		this.lastSynchronizedPob = lastSynchronizedPob;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, id, jiraIssueKey, lastSynchronizedJira, lastSynchronizedPob, modified, pobIssueKey, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final IncidentEntity other)) { return false; }
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(jiraIssueKey, other.jiraIssueKey) && Objects.equals(lastSynchronizedJira, other.lastSynchronizedJira) && Objects.equals(lastSynchronizedPob,
			other.lastSynchronizedPob) && Objects.equals(modified, other.modified) && Objects.equals(pobIssueKey, other.pobIssueKey) && (status == other.status);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("IncidentEntity [id=").append(id).append(", pobIssueKey=").append(pobIssueKey).append(", jiraIssueKey=").append(jiraIssueKey).append(", status=").append(status).append(", created=").append(created).append(", modified=").append(
			modified).append(", lastSynchronizedJira=").append(lastSynchronizedJira).append(", lastSynchronizedPob=").append(lastSynchronizedPob).append("]");
		return builder.toString();
	}
}
