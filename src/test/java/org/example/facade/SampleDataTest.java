package org.example.facade;

import org.example.booking.BookingService;
import org.example.storage.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SampleDataTest {
    @TempDir Path directory;

    @Test
    void suppliedSampleUsesTheUnchangedStorageSchema() throws Exception {
        Path data = directory.resolve("bookings.json");
        Files.copy(Path.of("sample-data", "bookings.json"), data);
        EventFacade facade = new EventFacade(new BookingService(new JsonStorage(data)));
        assertEquals(2, facade.events().size());
        assertEquals(3, facade.bookings().size());
        assertEquals(new BigDecimal("5000.00"), facade.getSummary("concert").totalPrice());
        assertEquals(3, facade.getSummary("concert").availableSeats());
        assertThrows(IllegalStateException.class, () -> facade.addBooking("workshop", "Extra", 1));
    }
}
