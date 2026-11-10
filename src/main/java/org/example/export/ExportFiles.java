package org.example.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

final class ExportFiles {
    private ExportFiles() { }
    /** Write beside the destination, then replace it to avoid partially written reports. */
    static void write(Path path, String text) throws IOException {
        Objects.requireNonNull(path, "Export path");
        Path target = path.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".report-", ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
