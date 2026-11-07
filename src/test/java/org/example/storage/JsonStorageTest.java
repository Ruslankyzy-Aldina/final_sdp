package org.example.storage;

import org.example.booking.Booking;
import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStorageTest {
    @TempDir
    Path directory;

    @Test
    void restartRestoresEventsBookingsAndStatus() throws Exception {
        Path file = directory.resolve("data/bookings.json");
        BookingService firstRun = new BookingService(new JsonStorage(file));
        firstRun.addEvent(new Event("concert", "Концерт", 5, new BigDecimal("120.00")));
        Booking booking = firstRun.createBooking("concert", "Айдана", 2);
        firstRun.confirmBooking(booking.id());

        assertTrue(Files.readString(file).contains("Айдана"));
        BookingService secondRun = new BookingService(new JsonStorage(file));
        assertEquals(1, secondRun.events().size());
        assertEquals("Концерт", secondRun.events().getFirst().name());
        assertEquals(BookingStatus.CONFIRMED, secondRun.bookings().getFirst().status());
        assertEquals(new BigDecimal("120.00"), secondRun.bookings().getFirst().unitPrice());
        assertEquals(3, secondRun.availableSeats("concert"));

        secondRun.cancelBooking(booking.id());
        BookingService thirdRun = new BookingService(new JsonStorage(file));
        assertEquals(BookingStatus.CANCELLED, thirdRun.bookings().getFirst().status());
        assertEquals(5, thirdRun.availableSeats("concert"));
    }

    @Test
    void invalidJsonFailsInsteadOfErasingBookings() throws Exception {
        Path file = directory.resolve("bookings.json");
        Files.writeString(file, "{invalid json}");
        assertThrows(UncheckedIOException.class, () -> new JsonStorage(file).load());
        assertEquals("{invalid json}", Files.readString(file));
    }
}
