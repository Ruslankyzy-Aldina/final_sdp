package org.example.ui;

import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.facade.EventFacade;
import org.example.pricing.GroupDiscountPricing;
import org.example.pricing.RegularPricing;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SummaryPanelTest {
    @Test
    void summaryRefreshesAfterBookingAndStrategyChangesAndDisablesInvalidExport() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EventFacade facade = new EventFacade(new BookingService());
            facade.createEvent("event", "Event", 6, BigDecimal.TEN);
            SummaryPanel panel = new SummaryPanel(facade);
            JButton export = descendants(panel).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> "Экспорт / Export".equals(button.getText())).findFirst().orElseThrow();
            JTextArea preview = descendants(panel).filter(JTextArea.class::isInstance).map(JTextArea.class::cast).findFirst().orElseThrow();
            panel.refresh(null);
            assertFalse(export.isEnabled());
            var booking = facade.addBooking("event", "Customer", 2);
            panel.refresh("event");
            assertTrue(export.isEnabled());
            assertTrue(preview.getText().contains("TOTAL (KZT): 20.00"));
            facade.setPricingStrategy(new GroupDiscountPricing(), "Group");
            panel.refresh("event");
            assertFalse(export.isEnabled());
            assertTrue(preview.getText().contains("Group discount requires"));
            facade.setPricingStrategy(new RegularPricing(), "Regular");
            facade.changeStatus(booking.id(), BookingStatus.CANCELLED);
            panel.refresh("event");
            assertTrue(export.isEnabled());
            assertTrue(preview.getText().contains("TOTAL (KZT): 0.00"));
        });
    }

    private static Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component instanceof Container container
                ? Stream.of(container.getComponents()).flatMap(SummaryPanelTest::descendants) : Stream.empty());
    }
}
