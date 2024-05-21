package se.sundsvall.incidentmapper.service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("application.synchronization")
public record SynchronizationProperties(int clockSkewInSeconds, String tempFolder, String responsibleUserGroupInPob) {

}
