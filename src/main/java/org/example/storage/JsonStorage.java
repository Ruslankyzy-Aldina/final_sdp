package org.example.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class JsonStorage implements Repository {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonStorage(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath();
    }

    @Override
    public BookingSnapshot load() {
        if (Files.notExists(file)) {
            return BookingSnapshot.empty();
        }
        try {
            return mapper.readValue(file.toFile(), BookingSnapshot.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load bookings from " + file, exception);
        }
    }

    @Override
    public void save(BookingSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Path temporary = null;
        try {
            Path directory = file.getParent();
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "bookings-", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), snapshot);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save bookings to " + file, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The original save failure, if any, is more useful to the caller.
                }
            }
        }
    }
}
