package org.example.model;

import org.example.booking.Booking;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.example.pricing.PriceCalculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Immutable report snapshot built from the existing booking model. */
public record Summary(Event event, String pricing, List<String> participants,
                      List<BookingLine> bookings, long reservedSeats,
                      long availableSeats, BigDecimal totalPrice) {
    public Summary {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(pricing, "pricing");
        participants = List.copyOf(participants);
        bookings = List.copyOf(bookings);
        Objects.requireNonNull(totalPrice, "totalPrice");
    }

    public record BookingLine(Booking booking, BigDecimal price) { }

    public static Summary of(Event event, List<Booking> bookings,
                             PriceCalculator calculator, String pricing) {
        List<BookingLine> lines = bookings.stream()
                .filter(booking -> booking.eventId().equals(event.id()))
                .map(booking -> new BookingLine(booking,
                        booking.status() == BookingStatus.CANCELLED
                                ? new BigDecimal("0.00") : calculator.calculate(booking)))
                .toList();
        long reserved = lines.stream().map(BookingLine::booking)
                .filter(booking -> booking.status() != BookingStatus.CANCELLED)
                .mapToLong(Booking::seatCount).sum();
        return new Summary(event, pricing,
                lines.stream().map(line -> line.booking().customerName()).distinct().toList(),
                lines, reserved, event.capacity() - reserved,
                lines.stream().map(BookingLine::price).reduce(new BigDecimal("0.00"), BigDecimal::add));
    }
}
