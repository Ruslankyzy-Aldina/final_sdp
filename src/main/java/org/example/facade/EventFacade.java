package org.example.facade;

import org.example.booking.Booking;
import org.example.booking.BookingListener;
import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.example.booking.NotificationLogListener;
import org.example.export.DefaultExporterFactory;
import org.example.export.ExporterFactory;
import org.example.model.Summary;
import org.example.pricing.PriceCalculator;
import org.example.pricing.PricingStrategy;
import org.example.pricing.RegularPricing;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** GUI boundary over participant B's original service, persistence and patterns. */
public final class EventFacade {
    private final BookingService service;
    private final ExporterFactory exporters;
    private final PriceCalculator calculator = new PriceCalculator(new RegularPricing());
    private final NotificationLogListener log = new NotificationLogListener();
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
        previewPrice(eventId, customer, seats);
        return service.createBooking(eventId, customer, seats);
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
