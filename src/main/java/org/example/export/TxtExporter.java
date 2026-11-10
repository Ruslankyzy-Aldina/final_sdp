package org.example.export;

import org.example.model.Summary;

import java.io.IOException;
import java.nio.file.Path;

public final class TxtExporter implements ReportExporter {
    /** Shared with the summary screen, so preview and TXT export show the same data. */
    public static String render(Summary summary) {
        StringBuilder text = new StringBuilder("EVENT STUDIO — SUMMARY\n")
                .append("Event: ").append(summary.event().name())
                .append("\nID: ").append(summary.event().id())
                .append("\nCapacity: ").append(summary.event().capacity())
                .append("\nUnit price (KZT): ").append(summary.event().unitPrice())
                .append("\nPricing: ").append(summary.pricing())
                .append("\nReserved seats: ").append(summary.reservedSeats())
                .append("\nAvailable seats: ").append(summary.availableSeats())
                .append("\n\nPARTICIPANTS (customer names)\n");
        summary.participants().forEach(name -> text.append(name).append('\n'));
        text.append("\nBOOKINGS\n");
        for (Summary.BookingLine line : summary.bookings()) {
            var booking = line.booking();
            text.append(booking.id()).append(" | ").append(booking.customerName())
                    .append(" | seats: ").append(booking.seatCount())
                    .append(" | ").append(booking.status())
                    .append(" | amount (KZT): ").append(line.price()).append('\n');
        }
        return text.append("\nTOTAL (KZT): ").append(summary.totalPrice())
                .append("\nCancelled bookings remain in history with zero amount.\n")
                .append("Prices are calculated with the selected strategy; they are not stored quotes.\n")
                .toString();
    }

    @Override
    public void export(Summary summary, Path path) throws IOException {
        ExportFiles.write(path, render(summary));
    }
}
