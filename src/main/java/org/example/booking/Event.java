package org.example.booking;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * An event. New events are created through {@link EventBuilder}, which validates user input;
 * this constructor only guards structural invariants. {@code date} and {@code location} may be
 * absent for events stored before they were introduced.
 */
public record Event(
        String id,
        String name,
        int capacity,
        BigDecimal unitPrice,
        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(using = IsoDateJson.class)
        LocalDate date,
        String location
) {
    public Event {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Event id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        location = location == null ? "" : location.trim();
    }

    /** Event without date and location, kept for existing callers and stored data. */
    public Event(String id, String name, int capacity, BigDecimal unitPrice) {
        this(id, name, capacity, unitPrice, null, "");
    }

    public static EventBuilder builder() {
        return new EventBuilder();
    }
}
