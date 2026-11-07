package org.example.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Event(String id, String name, int capacity, BigDecimal unitPrice) {
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
    }
}
