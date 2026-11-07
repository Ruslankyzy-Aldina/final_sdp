package org.example.booking;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingLookupTest {
    @Test
    void serviceExposesStableLookupForUi() {
        BookingService service = new BookingService();
        Event event = new Event("event-1", "Event", 3, new BigDecimal("10.00"));
        service.addEvent(event);
        Booking booking = service.createBooking("event-1", "Aida", 1);

        assertEquals(event, service.findEvent("event-1"));
        assertEquals(booking, service.findBooking(booking.id()));
        assertThrows(IllegalArgumentException.class, () -> service.findEvent("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.findBooking("missing"));
    }
}
