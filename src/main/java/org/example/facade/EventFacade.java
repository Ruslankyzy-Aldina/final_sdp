package org.example.facade;

import org.example.booking.Booking;
import org.example.booking.BookingListener;
import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.example.booking.EventBuilder;
import org.example.booking.NotificationLogListener;
import org.example.booking.Participant;
import org.example.export.DefaultExporterFactory;
import org.example.export.ExporterFactory;
import org.example.model.Summary;
import org.example.pricing.PriceCalculator;
import org.example.pricing.PricingStrategy;
import org.example.pricing.RegularPricing;
import org.example.servicepackage.ServicePackage;
import org.example.validation.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** GUI boundary over participant B's original service, persistence and patterns. */
public final class EventFacade {
    private final BookingService service;
    private final ExporterFactory exporters;
    private final PriceCalculator calculator = new PriceCalculator(new RegularPricing());
    private final NotificationLogListener log = new NotificationLogListener();
    private final Map<String, ServicePackage> packages = new HashMap<>();
    private String pricing = "Обычная";

    public EventFacade(BookingService service) {
        this(service, new DefaultExporterFactory());
    }

    public EventFacade(BookingService service, ExporterFactory exporters) {
        this.service = Objects.requireNonNull(service, "service");
        this.exporters = Objects.requireNonNull(exporters, "exporters");
        service.subscribe(log);
    }

    public Event createEvent(String id, String name, int capacity, BigDecimal price) {
        Event event = new Event(id, name, capacity, price);
        service.addEvent(event);
        return event;
    }

    /** Creates an event from the Builder filled by the GUI; validation happens in {@code build()}. */
    public Event createEvent(EventBuilder builder) {
        Event event = Objects.requireNonNull(builder, "builder").build();
        service.addEvent(event);
        return event;
    }

    /** Edits an event keeping its id; capacity may not drop below already reserved seats. */
    public Event updateEvent(String eventId, EventBuilder builder) {
        Event existing = service.findEvent(eventId);
        Event updated = Objects.requireNonNull(builder, "builder").id(existing.id()).build();
        int reserved = service.reservedSeatCount(eventId);
        if (updated.capacity() < reserved) {
            throw new ValidationException("Вместимость не может быть меньше уже забронированных мест ("
                    + reserved + ")");
        }
        service.updateEvent(updated);
        return updated;
    }

    public void deleteEvent(String eventId) {
        service.findEvent(eventId);
        if (service.bookings().stream().anyMatch(booking -> booking.eventId().equals(eventId))) {
            throw new ValidationException("Нельзя удалить мероприятие, у которого есть брони");
        }
        service.removeEvent(eventId);
        packages.remove(eventId);
    }

    /** Decorator: wraps the base package with the selected extras and remembers it for the event. */
    public ServicePackage configurePackage(String eventId, BigDecimal baseCost,
                                           boolean catering, boolean photographer, boolean decor) {
        if (eventId == null || eventId.isBlank()) {
            throw new ValidationException("Выберите мероприятие для пакета");
        }
        service.findEvent(eventId);
        ServicePackage servicePackage = ServicePackage.configure(eventId, baseCost, catering, photographer, decor);
        packages.put(eventId, servicePackage);
        return servicePackage;
    }

    public Optional<ServicePackage> findPackage(String eventId) {
        return Optional.ofNullable(packages.get(eventId));
    }

    public List<Event> events() { return service.events(); }
    public List<Booking> bookings() { return service.bookings(); }
    public Event findEvent(String id) { return service.findEvent(id); }
    public Booking findBooking(String id) { return service.findBooking(id); }
    public int availableSeats(String id) { return service.availableSeats(id); }
    public List<String> notifications() { return log.notifications(); }
    public void subscribe(BookingListener listener) { service.subscribe(listener); }
    public void unsubscribe(BookingListener listener) { service.unsubscribe(listener); }

    public void setPricingStrategy(PricingStrategy strategy, String label) {
        Objects.requireNonNull(label, "label");
        calculator.setStrategy(strategy);
        pricing = label;
    }

    public BigDecimal calculate(Booking booking) { return calculator.calculate(booking); }

    public BigDecimal previewPrice(String eventId, String customer, int seats) {
        Event event = service.findEvent(eventId);
        return calculator.calculate(new Booking("preview", eventId, customer, seats,
                event.unitPrice(), BookingStatus.CREATED));
    }

    public Booking addBooking(String eventId, String customer, int seats) {
        Participant participant = new Participant(customer);
        if (seats <= 0) {
            throw new ValidationException("Количество мест должно быть больше нуля");
        }
        previewPrice(eventId, participant.name(), seats);
        return service.createBooking(eventId, participant.name(), seats);
    }

    public Booking changeStatus(String bookingId, BookingStatus status) {
        return service.changeStatus(bookingId, status);
    }

    public Summary getSummary(String eventId) {
        return Summary.of(service.findEvent(eventId), service.bookings(), calculator, pricing);
    }

    public void exportSummary(String eventId, String format, Path path) throws IOException {
        exportSummary(getSummary(eventId), format, path);
    }

    /** Export the exact immutable snapshot shown by the GUI. */
    public void exportSummary(Summary summary, String format, Path path) throws IOException {
        exporters.export(summary, path, format);
    }
}
