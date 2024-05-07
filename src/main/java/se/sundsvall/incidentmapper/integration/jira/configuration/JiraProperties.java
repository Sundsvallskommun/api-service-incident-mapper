package se.sundsvall.incidentmapper.integration.jira.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.jira")
public record JiraProperties(String username, String password, String url) {

}
