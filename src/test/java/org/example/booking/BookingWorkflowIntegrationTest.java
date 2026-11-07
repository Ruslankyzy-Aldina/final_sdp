package org.example.booking;

import org.example.pricing.GroupDiscountPricing;
import org.example.pricing.PriceCalculator;
import org.example.pricing.StudentDiscountPricing;
import org.example.storage.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingWorkflowIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void mainScenarioCoversStrategyObserverCapacityAndRestart() {
        BookingService service = new BookingService(new JsonStorage(directory.resolve("bookings.json")));
        service.addEvent(new Event("conference", "Конференция", 10, new BigDecimal("1000.00")));

        List<String> events = new ArrayList<>();
        service.subscribe((booking, oldStatus, newStatus) ->
                events.add((oldStatus == null ? "NONE" : oldStatus) + " -> " + newStatus));

        Booking booking = service.createBooking("conference", "Aida", 5);
        PriceCalculator calculator = new PriceCalculator(new StudentDiscountPricing());
        assertEquals(new BigDecimal("4500.00"), calculator.calculate(booking));
        calculator.setStrategy(new GroupDiscountPricing());
        assertEquals(new BigDecimal("4250.00"), calculator.calculate(booking));

        service.confirmBooking(booking.id());
        service.cancelBooking(booking.id());
        assertEquals(List.of("NONE -> CREATED", "CREATED -> CONFIRMED", "CONFIRMED -> CANCELLED"), events);
        assertEquals(10, service.availableSeats("conference"));

        BookingService restarted = new BookingService(new JsonStorage(directory.resolve("bookings.json")));
        assertEquals(BookingStatus.CANCELLED, restarted.bookings().getFirst().status());
        assertEquals("Aida", restarted.bookings().getFirst().customerName());
        assertTrue(restarted.events().stream().anyMatch(event -> event.id().equals("conference")));
    }
}
