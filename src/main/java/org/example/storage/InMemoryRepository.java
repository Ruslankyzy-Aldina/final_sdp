package org.example.storage;

import java.util.Objects;

public final class InMemoryRepository implements Repository {
    private BookingSnapshot snapshot = BookingSnapshot.empty();

    @Override
    public BookingSnapshot load() {
        return snapshot;
    }

    @Override
    public void save(BookingSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }
}
