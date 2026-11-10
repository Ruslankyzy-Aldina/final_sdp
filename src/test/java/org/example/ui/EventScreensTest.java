package org.example.ui;

import org.example.booking.BookingService;
import org.example.facade.EventFacade;
import org.example.servicepackage.ServicePackage;
import org.example.validation.ValidationException;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Event list and package screens driven through their form fields on the Swing EDT. */
class EventScreensTest {
    @Test
    void eventListCreatesThroughBuilderEditsAndNotifies() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EventFacade facade = new EventFacade(new BookingService());
            AtomicInteger changes = new AtomicInteger();
            EventListPanel panel = new EventListPanel(facade, changes::incrementAndGet);

            panel.nameField.setText("Фестиваль");
            panel.dateField.setText(LocalDate.now().plusDays(5).toString());
            panel.locationField.setText("Алматы");
            panel.capacityField.setText("40");
            panel.priceField.setText("1500,50");
            panel.createEvent();

            assertEquals(1, panel.rowCount());
            assertEquals(1, changes.get());
            String id = facade.events().getFirst().id();
            assertEquals(new BigDecimal("1500.50"), facade.findEvent(id).unitPrice());

            panel.selectById(id);
            assertEquals("Фестиваль", panel.nameField.getText());
            panel.capacityField.setText("60");
            panel.updateEvent();
            assertEquals(60, facade.findEvent(id).capacity());
            assertEquals(2, changes.get());

            panel.nameField.setText("");
            assertEquals("Название мероприятия не может быть пустым",
                    assertThrows(ValidationException.class, panel::updateEvent).getMessage());
            panel.capacityField.setText("abc");
            assertThrows(ValidationException.class, panel::createEvent);
            assertEquals(1, facade.events().size());
        });
    }

    @Test
    void packageScreenSavesDecoratedPackage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EventFacade facade = new EventFacade(new BookingService());
            facade.createEvent("concert", "Концерт", 10, BigDecimal.TEN);
            PackagePanel panel = new PackagePanel(facade);

            panel.baseCostField.setText("100000");
            panel.cateringBox.doClick();
            panel.decorBox.doClick();
            assertEquals("Итого: 170000.00 KZT", panel.costLabel.getText());

            ServicePackage saved = panel.savePackage();
            assertEquals(saved, facade.findPackage("concert").orElseThrow());
            assertEquals("Состав: Базовый пакет + Кейтеринг + Декор", panel.descriptionLabel.getText());

            panel.baseCostField.setText("0");
            assertTrue(panel.costLabel.getText().startsWith("Ошибка:"));
            assertThrows(ValidationException.class, panel::savePackage);
        });
    }
}
