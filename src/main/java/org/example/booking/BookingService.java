package org.example.booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class BookingService {
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final Set<BookingListener> listeners = new LinkedHashSet<>();

    public void addEvent(Event event) {
        Objects.requireNonNull(event, "event");
        if (events.containsKey(event.id())) {
            throw new IllegalArgumentException("Event already exists: " + event.id());
        }
        events.put(event.id(), event);
    }

    public List<Event> events() {
        return List.copyOf(events.values());
    }

    public List<Booking> bookings() {
        return List.copyOf(bookings.values());
    }

    public Booking createBooking(String eventId, String customerName, int seatCount) {
        Event event = requireEvent(eventId);
        if (seatCount <= 0) {
            throw new IllegalArgumentException("Seat count must be positive");
        }
        if (reservedSeats(eventId) + seatCount > event.capacity()) {
            throw new IllegalStateException("Not enough seats for event: " + eventId);
        }
        Booking booking = new Booking(UUID.randomUUID().toString(), eventId, customerName,
                seatCount, event.unitPrice(), BookingStatus.CREATED);
        bookings.put(booking.id(), booking);
        notifyListeners(booking, null, BookingStatus.CREATED);
        return booking;
    }

    public Booking confirmBooking(String bookingId) {
        return changeStatus(bookingId, BookingStatus.CONFIRMED);
    }

    public Booking cancelBooking(String bookingId) {
        return changeStatus(bookingId, BookingStatus.CANCELLED);
    }

    public Booking changeStatus(String bookingId, BookingStatus newStatus) {
        Booking current = requireBooking(bookingId);
        Objects.requireNonNull(newStatus, "newStatus");
        if (current.status() == newStatus) {
            return current;
        }
        if (current.status() == BookingStatus.CANCELLED
                || newStatus == BookingStatus.CREATED) {
            throw new IllegalStateException("Invalid booking status transition");
        }
        Booking updated = current.withStatus(newStatus);
        bookings.put(bookingId, updated);
        notifyListeners(updated, current.status(), newStatus);
        return updated;
    }

    public int availableSeats(String eventId) {
        Event event = requireEvent(eventId);
        return event.capacity() - reservedSeats(eventId);
    }

    public void subscribe(BookingListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void unsubscribe(BookingListener listener) {
        listeners.remove(listener);
    }

    private int reservedSeats(String eventId) {
        return bookings.values().stream()
                .filter(booking -> booking.eventId().equals(eventId))
                .filter(booking -> booking.status() != BookingStatus.CANCELLED)
                .mapToInt(Booking::seatCount)
                .sum();
    }

    private Event requireEvent(String eventId) {
        Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Unknown event: " + eventId);
        }
        return event;
    }

    private Booking requireBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Unknown booking: " + bookingId);
        }
        return booking;
    }

    private void notifyListeners(Booking booking, BookingStatus oldStatus, BookingStatus newStatus) {
        for (BookingListener listener : new ArrayList<>(listeners)) {
            listener.onStatusChanged(booking, oldStatus, newStatus);
        }
    }
}
