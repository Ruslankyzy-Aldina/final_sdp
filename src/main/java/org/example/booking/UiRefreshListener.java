package org.example.booking;

import java.util.Objects;

public final class UiRefreshListener implements BookingListener {
    private final Runnable refresh;

    public UiRefreshListener(Runnable refresh) {
        this.refresh = Objects.requireNonNull(refresh, "refresh");
    }

    @Override
    public void onStatusChanged(Booking booking, BookingStatus oldStatus, BookingStatus newStatus) {
        refresh.run();
    }
}
