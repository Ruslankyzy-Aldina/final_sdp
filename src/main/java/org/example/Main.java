package org.example;

import org.example.booking.BookingService;
import org.example.booking.Event;
import org.example.storage.JsonStorage;
import org.example.ui.BookingFrame;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        BookingService service = new BookingService(new JsonStorage(Path.of("data", "bookings.json")));
        if (service.events().isEmpty()) {
            service.addEvent(new Event("concert", "Концерт", 50, new BigDecimal("5000.00")));
            service.addEvent(new Event("workshop", "Мастер-класс", 20, new BigDecimal("3000.00")));
        }
        SwingUtilities.invokeLater(() -> new BookingFrame(service).setVisible(true));
    }
}
