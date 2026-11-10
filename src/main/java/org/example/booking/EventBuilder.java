package org.example.booking;

import org.example.validation.ValidationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Builder: assembles an {@link Event} step by step and validates all fields together in
 * {@link #build()}, so the user sees every problem at once.
 */
public final class EventBuilder {
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_CAPACITY = 10_000;

    private final Clock clock;
    private String id;
    private String name;
    private LocalDate date;
    private String location;
    private Integer capacity;
    private BigDecimal price;

    public EventBuilder() {
        this(Clock.systemDefaultZone());
    }

    /** The clock decides what "today" is for the date-in-the-past rule. */
    public EventBuilder(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Pre-fills the builder from an existing event, e.g. for editing. */
    public static EventBuilder from(Event event, Clock clock) {
        return new EventBuilder(clock)
                .id(event.id())
                .name(event.name())
                .date(event.date())
                .location(event.location())
                .capacity(event.capacity())
                .price(event.unitPrice());
    }

    /** Optional; a unique id is generated when omitted. */
    public EventBuilder id(String id) {
        this.id = id;
        return this;
    }

    public EventBuilder name(String name) {
        this.name = name;
        return this;
    }

    public EventBuilder date(LocalDate date) {
        this.date = date;
        return this;
    }

    public EventBuilder location(String location) {
        this.location = location;
        return this;
    }

    public EventBuilder capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /** Ticket price per seat in KZT. */
    public EventBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public Event build() {
        List<String> errors = new ArrayList<>();
        String cleanName = trim(name);
        String cleanLocation = trim(location);

        if (cleanName.isEmpty()) {
            errors.add("Название мероприятия не может быть пустым");
        } else if (cleanName.length() > MAX_NAME_LENGTH) {
            errors.add("Название мероприятия не может быть длиннее " + MAX_NAME_LENGTH + " символов");
        }
        if (date == null) {
            errors.add("Укажите дату мероприятия");
        } else if (date.isBefore(LocalDate.now(clock))) {
            errors.add("Дата мероприятия не может быть в прошлом");
        }
        if (cleanLocation.isEmpty()) {
            errors.add("Место проведения не может быть пустым");
        }
        if (capacity == null) {
            errors.add("Укажите вместимость");
        } else if (capacity <= 0) {
            errors.add("Вместимость должна быть больше нуля");
        } else if (capacity > MAX_CAPACITY) {
            errors.add("Вместимость не может превышать " + MAX_CAPACITY);
        }
        if (price == null) {
            errors.add("Укажите цену билета");
        } else if (price.signum() <= 0) {
            errors.add("Цена билета должна быть больше нуля");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String eventId = trim(id).isEmpty() ? generateId(cleanName) : trim(id);
        return new Event(eventId, cleanName, capacity, price, date, cleanLocation);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String generateId(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return slug.isEmpty() ? "event-" + suffix : slug + "-" + suffix;
    }
}
