package org.example.ui;

import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BookingFrameIntegrationTest {
    @Test
    void originalWindowSharesBookingsObserversAndStrategyWithSummary() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Requires a desktop graphics environment");
        SwingUtilities.invokeAndWait(() -> {
            BookingService service = new BookingService();
            service.addEvent(new Event("event", "Event", 5, BigDecimal.TEN));
            BookingFrame frame = new BookingFrame(service);
            try {
                List<Component> controls = descendants(frame).toList();
                JTabbedPane tabs = first(controls, JTabbedPane.class);
                assertEquals(4, tabs.getTabCount());
                assertEquals("Итог и экспорт", tabs.getTitleAt(1));
                assertEquals("Мероприятия", tabs.getTitleAt(2));
                assertEquals("Пакет услуг", tabs.getTitleAt(3));
                first(controls, JTextField.class).setText("Customer");
                button(controls, "Создать").doClick();
                assertEquals(1, service.bookings().size());
                JTable table = first(controls, JTable.class);
                assertEquals(1, table.getRowCount());
                var strategy = controls.stream().filter(JComboBox.class::isInstance)
                        .map(JComboBox.class::cast)
                        .filter(box -> box.getSelectedItem() instanceof PricingOption).findFirst().orElseThrow();
                strategy.setSelectedItem(PricingOption.STUDENT);
                JTextArea summary = first(descendants(tabs.getComponentAt(1)).toList(), JTextArea.class);
                assertTrue(summary.getText().contains("TOTAL (KZT): 9.00"));
                table.setRowSelectionInterval(0, 0);
                button(controls, "Подтвердить").doClick();
                assertEquals(BookingStatus.CONFIRMED, service.bookings().getFirst().status());
                button(controls, "Отменить").doClick();
                assertEquals(BookingStatus.CANCELLED, service.bookings().getFirst().status());
                assertEquals(5, service.availableSeats("event"));
                assertTrue(summary.getText().contains("TOTAL (KZT): 0.00"));
                JTextArea notifications = first(descendants(tabs.getComponentAt(0)).toList(), JTextArea.class);
                assertTrue(notifications.getText().contains("CONFIRMED"));
                assertTrue(notifications.getText().contains("CANCELLED"));
            } finally {
                frame.dispose();
            }
        });
    }

    private static JButton button(List<Component> controls, String label) {
        return controls.stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> label.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T> T first(List<Component> controls, Class<T> type) {
        return controls.stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component instanceof Container container
                ? Stream.of(container.getComponents()).flatMap(BookingFrameIntegrationTest::descendants) : Stream.empty());
    }
}
