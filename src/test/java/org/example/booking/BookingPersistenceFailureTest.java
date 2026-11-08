package org.example.booking;

import org.example.storage.BookingSnapshot;
import org.example.storage.Repository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingPersistenceFailureTest {
    @Test
    void failedEventSaveRollsBackTheInMemoryState() {
        BookingService service = new BookingService(new FailingRepository());
        Event event = new Event("event-1", "Event", 3, new BigDecimal("10.00"));

        assertThrows(IllegalStateException.class, () -> service.addEvent(event));
        assertEquals(0, service.events().size());
    }

    @Test
    void failedBookingSaveRollsBackTheInMemoryState() {
        ToggleRepository repository = new ToggleRepository();
        BookingService service = new BookingService(repository);
        service.addEvent(new Event("event-1", "Event", 3, new BigDecimal("10.00")));
        repository.fail = true;

        assertThrows(IllegalStateException.class,
                () -> service.createBooking("event-1", "Aida", 1));
        assertEquals(0, service.bookings().size());
        assertEquals(3, service.availableSeats("event-1"));
    }

    private static class FailingRepository implements Repository {
        @Override
        public BookingSnapshot load() {
            return BookingSnapshot.empty();
        }

        @Override
        public void save(BookingSnapshot snapshot) {
            throw new IllegalStateException("Storage unavailable");
        }
    }

    private static final class ToggleRepository extends FailingRepository {
        private boolean fail;

        @Override
        public void save(BookingSnapshot snapshot) {
            if (fail) {
                super.save(snapshot);
            }
        }
    }
}
