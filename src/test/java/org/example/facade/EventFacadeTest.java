package org.example.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.booking.Booking;
import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.model.Summary;
import org.example.pricing.GroupDiscountPricing;
import org.example.pricing.RegularPricing;
import org.example.pricing.StudentDiscountPricing;
import org.example.storage.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventFacadeTest {
    @TempDir Path directory;

    private EventFacade facade() {
        EventFacade facade = new EventFacade(new BookingService(new JsonStorage(directory.resolve("bookings.json"))));
        facade.createEvent("event", "Концерт <script>alert('x')</script> & \"Ә\"", 8, new BigDecimal("1000.00"));
        return facade;
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "json", "html"})
    void scenario7ExportsSelectedFormatWithCorrectTotalsAndUnicode(String format) throws Exception {
        EventFacade facade = facade();
        facade.addBooking("event", "Әсел <b> & \"А\"\nnext\tline", 5);
        Booking cancelled = facade.addBooking("event", "Cancelled", 2);
        facade.changeStatus(cancelled.id(), BookingStatus.CANCELLED);
        facade.setPricingStrategy(new StudentDiscountPricing(), "Студенческая");
        Path file = directory.resolve("summary." + format);
        facade.exportSummary("event", format, file);
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("4500.00"));
        assertTrue(text.contains("CANCELLED"));
        assertTrue(text.contains("Әсел"));
        if (format.equals("json")) {
            var report = new ObjectMapper().readTree(text);
            var summary = report.get("summary");
            assertEquals("KZT", report.get("currency").asText());
            assertEquals(5, summary.get("reservedSeats").asInt());
            assertEquals(3, summary.get("availableSeats").asInt());
            assertEquals(2, summary.get("bookings").size());
            assertEquals("Әсел <b> & \"А\"\nnext\tline", summary.get("participants").get(0).asText());
            assertEquals(0, summary.get("bookings").get(1).get("price").decimalValue().signum());
        } else if (format.equals("html")) {
            assertFalse(text.contains("<script>"));
            assertFalse(text.contains("<b>"));
            assertTrue(text.contains("&lt;script&gt;"));
            assertTrue(text.contains("&amp;"));
            assertTrue(text.contains("&quot;"));
        }
    }

    @Test
    void scenario8RejectsOverCapacityWithoutChangingStorageOrObservers() throws Exception {
        EventFacade facade = facade();
        List<BookingStatus> notifications = new ArrayList<>();
        facade.subscribe((booking, oldStatus, newStatus) -> notifications.add(newStatus));
        Booking booking = facade.addBooking("event", "Full event", 8);
        String saved = Files.readString(directory.resolve("bookings.json"));
        assertThrows(IllegalStateException.class, () -> facade.addBooking("event", "Overflow", 1));
        assertEquals(saved, Files.readString(directory.resolve("bookings.json")));
        assertEquals(1, facade.bookings().size());
        assertEquals(0, facade.availableSeats("event"));
        assertEquals(List.of(BookingStatus.CREATED), notifications);
        facade.changeStatus(booking.id(), BookingStatus.CANCELLED);
        facade.addBooking("event", "Replacement", 8);
        assertEquals(List.of(BookingStatus.CREATED, BookingStatus.CANCELLED, BookingStatus.CREATED), notifications);
        EventFacade restarted = new EventFacade(new BookingService(new JsonStorage(directory.resolve("bookings.json"))));
        assertEquals(facade.getSummary("event"), restarted.getSummary("event"));
    }

    @Test
    void summaryIsAnImmutableSnapshotAndFiltersOtherEvents() {
        EventFacade facade = facade();
        facade.createEvent("other", "Other", 10, BigDecimal.ONE);
        facade.addBooking("other", "Other customer", 4);
        Booking first = facade.addBooking("event", "Customer", 2);
        facade.addBooking("event", "Customer", 1);
        Summary snapshot = facade.getSummary("event");
        assertEquals(List.of("Customer"), snapshot.participants());
        assertEquals(2, snapshot.bookings().size());
        assertEquals(new BigDecimal("3000.00"), snapshot.totalPrice());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.bookings().clear());
        facade.changeStatus(first.id(), BookingStatus.CANCELLED);
        assertEquals(new BigDecimal("3000.00"), snapshot.totalPrice());
        assertEquals(new BigDecimal("1000.00"), facade.getSummary("event").totalPrice());
    }

    @Test
    void strategySelectionMatchesExistingRulesAndDoesNotPersistQuotes() {
        EventFacade facade = facade();
        facade.addBooking("event", "Group", 5);
        facade.setPricingStrategy(new GroupDiscountPricing(), "Group");
        assertEquals(new BigDecimal("4250.00"), facade.getSummary("event").totalPrice());
        assertThrows(IllegalArgumentException.class, () -> facade.addBooking("event", "Too small", 1));
        assertEquals(1, facade.bookings().size());
        facade.setPricingStrategy(new RegularPricing(), "Regular");
        assertEquals(new BigDecimal("5000.00"), facade.getSummary("event").totalPrice());
        facade.addBooking("event", "Individual", 1);
        facade.setPricingStrategy(new GroupDiscountPricing(), "Group");
        assertThrows(IllegalArgumentException.class, () -> facade.getSummary("event"));
    }

    @Test
    void emptyEventAndCancelledSmallBookingWorkWithGroupPricing() {
        EventFacade facade = facade();
        assertEquals(new BigDecimal("0.00"), facade.getSummary("event").totalPrice());
        Booking booking = facade.addBooking("event", "Cancelled", 1);
        facade.changeStatus(booking.id(), BookingStatus.CANCELLED);
        facade.setPricingStrategy(new GroupDiscountPricing(), "Group");
        assertEquals(new BigDecimal("0.00"), facade.getSummary("event").totalPrice());
        assertEquals(8, facade.getSummary("event").availableSeats());
    }

    @Test
    void exportingSnapshotDoesNotRecalculateAfterStrategyChanges() throws Exception {
        EventFacade facade = facade();
        facade.addBooking("event", "Customer", 5);
        Summary snapshot = facade.getSummary("event");
        facade.setPricingStrategy(new StudentDiscountPricing(), "Student");
        Path report = directory.resolve("snapshot.txt");
        facade.exportSummary(snapshot, "txt", report);
        assertTrue(Files.readString(report).contains("TOTAL (KZT): 5000.00"));
    }

    @Test
    void invalidFormatAndWriteFailuresLeaveExistingFilesAndBookingsUntouched() throws Exception {
        EventFacade facade = facade();
        Path report = directory.resolve("existing.txt");
        Files.writeString(report, "keep");
        assertThrows(IllegalArgumentException.class, () -> facade.exportSummary("event", "pdf", report));
        assertEquals("keep", Files.readString(report));
        assertThrows(IOException.class, () -> facade.exportSummary("event", "txt", directory.resolve("missing/report.txt")));
        Path occupiedDirectory = Files.createDirectory(directory.resolve("not-a-file"));
        Files.writeString(occupiedDirectory.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> facade.exportSummary("event", "json", occupiedDirectory));
        assertEquals("keep", Files.readString(occupiedDirectory.resolve("keep.txt")));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(file -> file.getFileName().toString().startsWith(".report-")));
        }
        assertTrue(facade.bookings().isEmpty());
        facade.exportSummary("event", "txt", report);
        assertTrue(Files.readString(report).contains("TOTAL (KZT): 0.00"));
    }
}
