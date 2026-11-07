package org.example.booking;

public interface BookingListener {
    // oldStatus is null when a booking has just been created.
    void onStatusChanged(Booking booking, BookingStatus oldStatus, BookingStatus newStatus);
}
