package org.example.export;

import java.util.Locale;

/** Concrete Creator for the three formats offered by the application. */
public final class DefaultExporterFactory extends ExporterFactory {
    @Override public ReportExporter createExporter(String format) {
        if (format == null) throw new IllegalArgumentException("Export format is required");
        return switch (format.trim().toLowerCase(Locale.ROOT)) {
            case "txt" -> new TxtExporter();
            case "json" -> new JsonExporter();
            case "html" -> new HtmlExporter();
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        };
    }
}
