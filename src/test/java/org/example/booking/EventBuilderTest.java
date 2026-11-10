package org.example.booking;

import org.example.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class EventBuilderTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    private EventBuilder valid() {
        return new EventBuilder(CLOCK).name("  Конференция  ").date(TODAY.plusDays(10))
                .location(" Астана ").capacity(100).price(new BigDecimal("2500"));
    }

    @Test
    void buildsEventWithAllFieldsStepByStep() {
        Event event = valid().id("conf").build();

        assertEquals("conf", event.id());
        assertEquals("Конференция", event.name());
        assertEquals(TODAY.plusDays(10), event.date());
        assertEquals("Астана", event.location());
        assertEquals(100, event.capacity());
        assertEquals(new BigDecimal("2500.00"), event.unitPrice());
    }

    @Test
    void generatesUniqueIdWhenNotGivenAndAcceptsToday() {
        Event first = valid().date(TODAY).build();
        Event second = valid().build();

        assertFalse(first.id().isBlank());
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void fromExistingEventKeepsAllValues() {
        Event original = valid().id("conf").build();
        assertEquals(original, EventBuilder.from(original, CLOCK).build());
    }

    @Test
    void collectsEveryViolatedRuleAtOnce() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> new EventBuilder(CLOCK).name(" ").date(TODAY.minusDays(1)).location("")
                        .capacity(0).price(new BigDecimal("-1")).build());

        assertEquals(5, error.getErrors().size());
        assertTrue(error.getErrors().contains("Название мероприятия не может быть пустым"));
        assertTrue(error.getErrors().contains("Дата мероприятия не может быть в прошлом"));
        assertTrue(error.getErrors().contains("Место проведения не может быть пустым"));
        assertTrue(error.getErrors().contains("Вместимость должна быть больше нуля"));
        assertTrue(error.getErrors().contains("Цена билета должна быть больше нуля"));
        assertTrue(error.getMessage().contains("Название мероприятия не может быть пустым"));
    }

    @Test
    void reportsMissingFields() {
        ValidationException error = assertThrows(ValidationException.class, () -> new EventBuilder(CLOCK).build());

        assertEquals(5, error.getErrors().size());
        assertTrue(error.getErrors().contains("Укажите дату мероприятия"));
        assertTrue(error.getErrors().contains("Укажите вместимость"));
        assertTrue(error.getErrors().contains("Укажите цену билета"));
    }

    @Test
    void enforcesUpperLimits() {
        assertThrows(ValidationException.class, () -> valid().name("x".repeat(101)).build());
        assertThrows(ValidationException.class, () -> valid().capacity(EventBuilder.MAX_CAPACITY + 1).build());
        assertDoesNotThrow(() -> valid().capacity(EventBuilder.MAX_CAPACITY).build());
    }

    @Test
    void validationExceptionIsAnIllegalArgumentForExistingErrorHandling() {
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(ValidationException.class, () -> valid().name("").build()));
    }
}
