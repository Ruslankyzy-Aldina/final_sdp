package org.example.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Summary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonExporter implements ReportExporter {
    @Override
    public void export(Summary summary, Path path) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("currency", "KZT");
        report.put("priceBasis", "Selected strategy; cancelled bookings contribute zero; quotes are not persisted");
        report.put("summary", summary);
        ExportFiles.write(path, new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report) + "\n");
    }
}
