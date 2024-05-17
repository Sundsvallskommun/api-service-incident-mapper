package se.sundsvall.incidentmapper.integration.jira;

public class JiraIntegrationException extends RuntimeException {

	private static final long serialVersionUID = -8703091317878200039L;

	public JiraIntegrationException(Throwable throwable) {
		super(throwable);
	}
}
