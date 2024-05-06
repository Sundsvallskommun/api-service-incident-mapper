package se.sundsvall.incidentmapper.integration.db.converter;

import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.sundsvall.incidentmapper.integration.db.model.enums.Status;

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Status, String> {

	@Override
	public String convertToDatabaseColumn(Status attribute) {
		return Optional.ofNullable(attribute)
			.map(Object::toString)
			.orElse(null);
	}

	@Override
	public Status convertToEntityAttribute(String columnValue) {
		return Status.valueOf(columnValue);
	}
}
