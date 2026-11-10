package org.example.booking;

import org.example.storage.BookingSnapshot;
import org.example.storage.InMemoryRepository;
import org.example.storage.Repository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingServiceEventCrudTest {
    private static Event event(String id, int capacity) {
        return new Event(id, "Event " + id, capacity, BigDecimal.TEN, LocalDate.of(2030, 1, 1), "Hall");
    }

    @Test
    void updatesAndRemovesEventsKeepingOrder() {
        BookingService service = new BookingService();
        service.addEvent(event("a", 5));
        service.addEvent(event("b", 5));
        service.addEvent(event("c", 5));

        service.updateEvent(event("b", 9));
        service.removeEvent("a");

        assertEquals(List.of("b", "c"), service.events().stream().map(Event::id).toList());
        assertEquals(9, service.findEvent("b").capacity());
    }

    @Test
    void guardsCapacityAndReferencedEvents() {
        BookingService service = new BookingService();
        service.addEvent(event("a", 5));
        service.createBooking("a", "Client", 4);

        assertEquals(4, service.reservedSeatCount("a"));
        assertThrows(IllegalStateException.class, () -> service.updateEvent(event("a", 3)));
        assertThrows(IllegalStateException.class, () -> service.removeEvent("a"));
        assertThrows(IllegalArgumentException.class, () -> service.updateEvent(event("missing", 3)));
        assertEquals(5, service.findEvent("a").capacity());
    }

    @Test
    void rollsBackWhenSavingFails() {
        boolean[] fail = {false};
        InMemoryRepository delegate = new InMemoryRepository();
        BookingService service = new BookingService(new Repository() {
            @Override
            public BookingSnapshot load() {
                return delegate.load();
            }

            @Override
            public void save(BookingSnapshot snapshot) {
                if (fail[0]) {
                    throw new IllegalStateException("disk full");
                }
                delegate.save(snapshot);
            }
        });
        service.addEvent(event("a", 5));
        service.addEvent(event("b", 5));
        fail[0] = true;

        assertThrows(IllegalStateException.class, () -> service.updateEvent(event("a", 8)));
        assertThrows(IllegalStateException.class, () -> service.removeEvent("a"));

        assertEquals(List.of("a", "b"), service.events().stream().map(Event::id).toList());
        assertEquals(5, service.findEvent("a").capacity());
    }
}
