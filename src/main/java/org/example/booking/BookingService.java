package org.example.booking;

import org.example.storage.BookingSnapshot;
import org.example.storage.InMemoryRepository;
import org.example.storage.Repository;

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
    private final Repository repository;

    public BookingService() {
        this(new InMemoryRepository());
    }

    public BookingService(Repository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        BookingSnapshot saved = Objects.requireNonNull(repository.load(), "repository.load()");
        for (Event event : saved.events()) {
            if (events.putIfAbsent(event.id(), event) != null) {
                throw new IllegalStateException("Duplicate event in repository: " + event.id());
            }
        }
        for (Booking booking : saved.bookings()) {
            if (!events.containsKey(booking.eventId())) {
                throw new IllegalStateException("Booking references unknown event: " + booking.eventId());
            }
            if (bookings.putIfAbsent(booking.id(), booking) != null) {
                throw new IllegalStateException("Duplicate booking in repository: " + booking.id());
            }
        }
        for (Event event : events.values()) {
            if (reservedSeats(event.id()) > event.capacity()) {
                throw new IllegalStateException("Stored bookings exceed capacity: " + event.id());
            }
        }
    }

    public void addEvent(Event event) {
        Objects.requireNonNull(event, "event");
        if (events.containsKey(event.id())) {
            throw new IllegalArgumentException("Event already exists: " + event.id());
        }
        events.put(event.id(), event);
        try {
            persist();
        } catch (RuntimeException exception) {
            events.remove(event.id());
            throw exception;
        }
    }

    /** Replaces an existing event; capacity cannot drop below the seats already reserved. */
    public void updateEvent(Event event) {
        Objects.requireNonNull(event, "event");
        Event previous = requireEvent(event.id());
        if (event.capacity() < reservedSeats(event.id())) {
            throw new IllegalStateException("Capacity is below reserved seats for event: " + event.id());
        }
        events.put(event.id(), event);
        try {
            persist();
        } catch (RuntimeException exception) {
            events.put(previous.id(), previous);
            throw exception;
        }
    }

    /** Removes an event; events referenced by any booking (including cancelled) are kept. */
    public void removeEvent(String eventId) {
        Event previous = requireEvent(eventId);
        if (bookings.values().stream().anyMatch(booking -> booking.eventId().equals(eventId))) {
            throw new IllegalStateException("Event has bookings: " + eventId);
        }
        Map<String, Event> before = new LinkedHashMap<>(events);
        events.remove(eventId);
        try {
            persist();
        } catch (RuntimeException exception) {
            events.clear();
            events.putAll(before);
            throw exception;
        }
    }

    public int reservedSeatCount(String eventId) {
        requireEvent(eventId);
        return reservedSeats(eventId);
    }

    public List<Event> events() {
        return List.copyOf(events.values());
    }

    public List<Booking> bookings() {
        return List.copyOf(bookings.values());
    }

    public Booking findBooking(String bookingId) {
        return requireBooking(bookingId);
    }

    public Event findEvent(String eventId) {
        return requireEvent(eventId);
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
        try {
            persist();
        } catch (RuntimeException exception) {
            bookings.remove(booking.id());
            throw exception;
        }
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
        try {
            persist();
        } catch (RuntimeException exception) {
            bookings.put(bookingId, current);
            throw exception;
        }
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

    private void persist() {
        repository.save(new BookingSnapshot(events(), bookings()));
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
