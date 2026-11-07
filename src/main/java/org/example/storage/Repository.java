package org.example.storage;

public interface Repository {
    BookingSnapshot load();

    void save(BookingSnapshot snapshot);
}
