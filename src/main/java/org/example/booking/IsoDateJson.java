package org.example.booking;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Reads ISO-8601 dates (yyyy-MM-dd) without adding the jackson-datatype-jsr310 dependency. */
final class IsoDateJson extends StdDeserializer<LocalDate> {
    IsoDateJson() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException exception) {
            return (LocalDate) context.handleWeirdStringValue(LocalDate.class, text,
                    "Expected ISO date yyyy-MM-dd");
        }
    }
}
