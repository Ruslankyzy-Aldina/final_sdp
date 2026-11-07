package org.example.storage;

import org.example.booking.Booking;
import org.example.booking.Event;

import java.util.List;
import java.util.Objects;

public record BookingSnapshot(List<Event> events, List<Booking> bookings) {
    public BookingSnapshot {
        events = List.copyOf(Objects.requireNonNull(events, "events"));
        bookings = List.copyOf(Objects.requireNonNull(bookings, "bookings"));
    }

    public static BookingSnapshot empty() {
        return new BookingSnapshot(List.of(), List.of());
    }
}
