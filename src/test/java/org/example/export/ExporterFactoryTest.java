package org.example.export;

import org.example.booking.BookingService;
import org.example.facade.EventFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ExporterFactoryTest {
    @ParameterizedTest
    @CsvSource({"txt,TxtExporter", "JSON,JsonExporter", "html,HtmlExporter", "' TXT ',TxtExporter"})
    void createsExpectedProduct(String format, String type) {
        assertEquals(type, new DefaultExporterFactory().createExporter(format).getClass().getSimpleName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"pdf", "   "})
    void rejectsUnsupportedFormats(String format) {
        assertThrows(IllegalArgumentException.class, () -> new DefaultExporterFactory().createExporter(format));
    }

    @Test
    void exportOperationUsesOverriddenFactoryMethod() throws Exception {
        AtomicBoolean called = new AtomicBoolean();
        ExporterFactory custom = new ExporterFactory() {
            @Override public ReportExporter createExporter(String format) {
                assertEquals("custom", format);
                return (summary, path) -> {
                    assertEquals("event", summary.event().id());
                    assertEquals(Path.of("unused"), path);
                    called.set(true);
                };
            }
        };
        EventFacade facade = new EventFacade(new BookingService(), custom);
        facade.createEvent("event", "Event", 5, BigDecimal.TEN);
        facade.exportSummary("event", "custom", Path.of("unused"));
        assertTrue(called.get());
    }
}
