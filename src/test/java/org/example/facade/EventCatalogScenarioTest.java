package org.example.facade;

import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.example.booking.EventBuilder;
import org.example.servicepackage.ServicePackage;
import org.example.storage.JsonStorage;
import org.example.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** User scenarios 1, 5 and 6 of participant A (Builder, Decorator, validation). */
class EventCatalogScenarioTest {
    @TempDir Path directory;

    private Path data() {
        return directory.resolve("bookings.json");
    }

    private EventFacade facade() {
        return new EventFacade(new BookingService(new JsonStorage(data())));
    }

    private static EventBuilder conference() {
        return new EventBuilder().name("Конференция IT").date(LocalDate.now().plusDays(20))
                .location("Астана, EXPO").capacity(10).price(new BigDecimal("2000.00"));
    }

    @Test
    void scenario1CreateConfigureBookEditAndDeleteSuccessfully() {
        EventFacade facade = facade();

        Event event = facade.createEvent(conference());
        ServicePackage servicePackage = facade.configurePackage(event.id(), new BigDecimal("100000"),
                true, true, false);
        facade.changeStatus(facade.addBooking(event.id(), "  Әсел  ", 3).id(), BookingStatus.CONFIRMED);
        Event edited = facade.updateEvent(event.id(), EventBuilder.from(event, java.time.Clock.systemDefaultZone())
                .location("Астана, Barys Arena").capacity(12));

        assertEquals(new BigDecimal("180000.00"), servicePackage.getCost());
        assertEquals("Базовый пакет + Кейтеринг + Фотограф", facade.findPackage(event.id()).orElseThrow().getDescription());
        assertEquals("Әсел", facade.bookings().getFirst().customerName());
        assertEquals(new BigDecimal("6000.00"), facade.getSummary(event.id()).totalPrice());
        assertEquals(9, facade.availableSeats(event.id()));

        EventFacade restarted = facade();
        Event stored = restarted.findEvent(event.id());
        assertEquals(edited, stored);
        assertEquals(LocalDate.now().plusDays(20), stored.date());
        assertEquals("Астана, Barys Arena", stored.location());

        Event spare = restarted.createEvent(conference().name("Лишнее мероприятие"));
        restarted.configurePackage(spare.id(), BigDecimal.TEN, false, false, true);
        restarted.deleteEvent(spare.id());
        assertEquals(1, restarted.events().size());
        assertTrue(restarted.findPackage(spare.id()).isEmpty());
    }

    @Test
    void scenario5EmptyEventNameIsRejectedWithMessageAndNothingIsSaved() {
        EventFacade facade = facade();

        ValidationException error = assertThrows(ValidationException.class,
                () -> facade.createEvent(conference().name("   ")));

        assertEquals("Название мероприятия не может быть пустым", error.getMessage());
        assertTrue(facade.events().isEmpty());
        assertFalse(Files.exists(data()));
    }

    @Test
    void scenario6ZeroOrNegativeCapacityOrPriceIsRejected() {
        EventFacade facade = facade();

        assertEquals("Вместимость должна быть больше нуля", assertThrows(ValidationException.class,
                () -> facade.createEvent(conference().capacity(0))).getMessage());
        assertEquals("Вместимость должна быть больше нуля", assertThrows(ValidationException.class,
                () -> facade.createEvent(conference().capacity(-5))).getMessage());
        assertEquals("Цена билета должна быть больше нуля", assertThrows(ValidationException.class,
                () -> facade.createEvent(conference().price(BigDecimal.ZERO))).getMessage());
        assertEquals("Цена билета должна быть больше нуля", assertThrows(ValidationException.class,
                () -> facade.createEvent(conference().price(new BigDecimal("-100")))).getMessage());
        assertTrue(facade.events().isEmpty());

        Event event = facade.createEvent(conference());
        assertThrows(ValidationException.class,
                () -> facade.configurePackage(event.id(), new BigDecimal("-1"), true, false, false));
        assertThrows(ValidationException.class,
                () -> facade.updateEvent(event.id(), EventBuilder.from(event, java.time.Clock.systemDefaultZone()).capacity(0)));
        assertTrue(facade.findPackage(event.id()).isEmpty());
        assertEquals(10, facade.findEvent(event.id()).capacity());
    }

    @Test
    void pastDateAndBusinessRulesAreReportedAsValidationErrors() {
        EventFacade facade = facade();
        assertEquals("Дата мероприятия не может быть в прошлом", assertThrows(ValidationException.class,
                () -> facade.createEvent(conference().date(LocalDate.now().minusDays(1)))).getMessage());

        Event event = facade.createEvent(conference());
        String bookingId = facade.addBooking(event.id(), "Дана", 4).id();

        assertTrue(assertThrows(ValidationException.class, () -> facade.updateEvent(event.id(),
                EventBuilder.from(event, java.time.Clock.systemDefaultZone()).capacity(3)))
                .getMessage().contains("забронированных мест (4)"));
        facade.changeStatus(bookingId, BookingStatus.CANCELLED);
        assertEquals("Нельзя удалить мероприятие, у которого есть брони",
                assertThrows(ValidationException.class, () -> facade.deleteEvent(event.id())).getMessage());
        assertEquals("Имя участника не может быть пустым",
                assertThrows(ValidationException.class, () -> facade.addBooking(event.id(), " ", 1)).getMessage());
        assertThrows(ValidationException.class, () -> facade.addBooking(event.id(), "Дана", 0));
        assertEquals(1, facade.events().size());
    }

    @Test
    void eventsStoredBeforeDateAndLocationStillLoad() {
        EventFacade facade = facade();
        facade.createEvent("legacy", "Старое событие", 5, BigDecimal.TEN);

        Event loaded = facade().findEvent("legacy");
        assertNull(loaded.date());
        assertEquals("", loaded.location());
    }
}
