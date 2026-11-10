package org.example.export;

import org.example.model.Summary;
import java.io.IOException;
import java.nio.file.Path;

/** Creator: subclasses decide which concrete exporter the operation uses. */
public abstract class ExporterFactory {
    public abstract ReportExporter createExporter(String format);

    public final void export(Summary summary, Path path, String format) throws IOException {
        createExporter(format).export(summary, path);
    }
}
