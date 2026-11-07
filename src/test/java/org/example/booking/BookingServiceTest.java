package org.example.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceTest {
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService();
        service.addEvent(new Event("concert", "Concert", 6, new BigDecimal("100.00")));
    }

    @Test
    void creationConfirmationAndCancellationNotifySubscribers() {
        List<String> transitions = new ArrayList<>();
        service.subscribe((booking, oldStatus, newStatus) ->
                transitions.add(oldStatus + " -> " + newStatus));
        NotificationLogListener log = new NotificationLogListener();
        service.subscribe(log);
        AtomicInteger refreshes = new AtomicInteger();
        service.subscribe(new UiRefreshListener(refreshes::incrementAndGet));

        Booking created = service.createBooking("concert", "Aida", 2);
        Booking confirmed = service.confirmBooking(created.id());
        Booking cancelled = service.cancelBooking(created.id());

        assertEquals(BookingStatus.CREATED, created.status());
        assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(List.of("null -> CREATED", "CREATED -> CONFIRMED", "CONFIRMED -> CANCELLED"),
                transitions);
        assertEquals(3, log.notifications().size());
        assertTrue(log.notifications().get(2).contains("CONFIRMED -> CANCELLED"));
        assertEquals(3, refreshes.get());
    }

    @Test
    void capacityIsReservedOnCreationAndReleasedOnCancellation() {
        Booking first = service.createBooking("concert", "Aida", 4);
        assertEquals(2, service.availableSeats("concert"));
        assertThrows(IllegalStateException.class,
                () -> service.createBooking("concert", "Dana", 3));
        assertEquals(1, service.bookings().size());

        service.cancelBooking(first.id());
        assertEquals(6, service.availableSeats("concert"));
        service.createBooking("concert", "Dana", 6);
        assertEquals(0, service.availableSeats("concert"));
    }

    @Test
    void invalidTransitionsDoNotNotifyAndUnsubscribeStopsNotifications() {
        List<BookingStatus> statuses = new ArrayList<>();
        BookingListener listener = (booking, oldStatus, newStatus) -> statuses.add(newStatus);
        service.subscribe(listener);
        Booking created = service.createBooking("concert", "Aida", 1);

        service.confirmBooking(created.id());
        service.confirmBooking(created.id());
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(created.id(), BookingStatus.CREATED));
        service.unsubscribe(listener);
        service.cancelBooking(created.id());
        assertThrows(IllegalStateException.class,
                () -> service.confirmBooking(created.id()));

        assertEquals(List.of(BookingStatus.CREATED, BookingStatus.CONFIRMED), statuses);
    }
}
