package org.example;

import org.example.booking.BookingService;
import org.example.booking.Event;
import org.example.storage.JsonStorage;
import org.example.ui.BookingFrame;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        BookingService service = new BookingService(new JsonStorage(Path.of("data", "bookings.json")));
        if (service.events().isEmpty()) {
            LocalDate today = LocalDate.now();
            service.addEvent(Event.builder().id("concert").name("Концерт").date(today.plusDays(30))
                    .location("Астана, Конгресс-холл").capacity(50).price(new BigDecimal("5000.00")).build());
            service.addEvent(Event.builder().id("workshop").name("Мастер-класс").date(today.plusDays(14))
                    .location("Алматы, коворкинг").capacity(20).price(new BigDecimal("3000.00")).build());
        }
        SwingUtilities.invokeLater(() -> new BookingFrame(service).setVisible(true));
    }
}
