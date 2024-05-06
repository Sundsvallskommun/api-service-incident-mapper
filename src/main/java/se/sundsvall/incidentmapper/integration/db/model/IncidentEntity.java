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
import se.sundsvall.incidentmapper.integration.db.listener.IncidentEntityListener;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;

@Entity
@Table(
	name = "incident",
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

	@Column(name = "jira_issue_last_modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime jiraIssueLastModified;

	@Column(name = "pob_issue_last_modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime pobIssueLastModified;

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

	public OffsetDateTime getJiraIssueLastModified() {
		return jiraIssueLastModified;
	}

	public void setJiraIssueLastModified(OffsetDateTime jiraIssueLastModified) {
		this.jiraIssueLastModified = jiraIssueLastModified;
	}

	public IncidentEntity withJiraIssueLastModified(OffsetDateTime jiraIssueLastModified) {
		this.jiraIssueLastModified = jiraIssueLastModified;
		return this;
	}

	public OffsetDateTime getPobIssueLastModified() {
		return pobIssueLastModified;
	}

	public void setPobIssueLastModified(OffsetDateTime pobIssueLastModified) {
		this.pobIssueLastModified = pobIssueLastModified;
	}

	public IncidentEntity withPobIssueLastModified(OffsetDateTime pobIssueLastModified) {
		this.pobIssueLastModified = pobIssueLastModified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, id, jiraIssueKey, jiraIssueLastModified, modified, pobIssueKey, pobIssueLastModified, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final IncidentEntity other)) { return false; }
		return Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(jiraIssueKey, other.jiraIssueKey) && Objects.equals(jiraIssueLastModified, other.jiraIssueLastModified) && Objects.equals(modified, other.modified)
			&& Objects.equals(pobIssueKey, other.pobIssueKey) && Objects.equals(pobIssueLastModified, other.pobIssueLastModified) && (status == other.status);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("IncidentEntity [id=").append(id).append(", pobIssueKey=").append(pobIssueKey).append(", jiraIssueKey=").append(jiraIssueKey).append(", status=").append(status).append(", created=").append(created).append(", modified=").append(
			modified).append(", jiraIssueLastModified=").append(jiraIssueLastModified).append(", pobIssueLastModified=").append(pobIssueLastModified).append("]");
		return builder.toString();
	}

}
