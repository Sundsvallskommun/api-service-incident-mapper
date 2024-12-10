package se.sundsvall.incidentmapper.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;
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
		return Optional.ofNullable(columnValue)
			.map(Status::valueOf)
			.orElse(null);
	}
}
