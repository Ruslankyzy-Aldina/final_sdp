package org.example.booking;

import java.util.ArrayList;
import java.util.List;

public final class NotificationLogListener implements BookingListener {
    private final List<String> notifications = new ArrayList<>();

    @Override
    public void onStatusChanged(Booking booking, BookingStatus oldStatus, BookingStatus newStatus) {
        notifications.add("Booking " + booking.id() + ": "
                + (oldStatus == null ? "NEW" : oldStatus)
                + " -> " + newStatus);
    }

    public List<String> notifications() {
        return List.copyOf(notifications);
    }
}
