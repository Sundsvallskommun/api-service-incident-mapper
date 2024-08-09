package se.sundsvall.incidentmapper.integration.pob.model;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class Mail {

	private int numberOfAttachments;
	private String id;
	private String from;
	private String to;
	private String replyTo;
	private String sendDate;
	private String subject;
	private String body;
	private List<File> attachments;

	public static Mail create() {
		return new Mail();
	}

	public int getNumberOfAttachments() {
		return numberOfAttachments;
	}

	public void setNumberOfAttachments(int numberOfAttachments) {
		this.numberOfAttachments = numberOfAttachments;
	}

	public Mail withNumberOfAttachments(int numberOfAttachments) {
		this.numberOfAttachments = numberOfAttachments;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Mail withId(String id) {
		this.id = id;
		return this;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public Mail withFrom(String from) {
		this.from = from;
		return this;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Mail withTo(String to) {
		this.to = to;
		return this;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public Mail withReplyTo(String replyTo) {
		this.replyTo = replyTo;
		return this;
	}

	public String getSendDate() {
		return sendDate;
	}

	public void setSendDate(String sendDate) {
		this.sendDate = sendDate;
	}

	public Mail withSendDate(String sendDate) {
		this.sendDate = sendDate;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Mail withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Mail withBody(String body) {
		this.body = body;
		return this;
	}

	public List<File> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<File> attachments) {
		this.attachments = attachments;
	}

	public Mail withAttachments(List<File> attachments) {
		this.attachments = attachments;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attachments, body, from, id, numberOfAttachments, replyTo, sendDate, subject, to);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final Mail other)) { return false; }
		return Objects.equals(attachments, other.attachments) && Objects.equals(body, other.body) && Objects.equals(from, other.from) && Objects.equals(id, other.id) && (numberOfAttachments == other.numberOfAttachments) && Objects.equals(replyTo,
			other.replyTo) && Objects.equals(sendDate, other.sendDate) && Objects.equals(subject, other.subject) && Objects.equals(to, other.to);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Mail [numberOfAttachments=").append(numberOfAttachments).append(", id=").append(id).append(", from=").append(from).append(", to=").append(to).append(", replyTo=").append(replyTo).append(", sendDate=").append(sendDate).append(
			", subject=").append(subject).append(", body=").append(body).append(", attachments=").append(attachments).append("]");
		return builder.toString();
	}
}
