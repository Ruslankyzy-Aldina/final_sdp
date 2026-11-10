package org.example.export;

import org.example.model.Summary;
import java.io.IOException;
import java.nio.file.Path;

public interface ReportExporter {
    void export(Summary summary, Path path) throws IOException;
}
