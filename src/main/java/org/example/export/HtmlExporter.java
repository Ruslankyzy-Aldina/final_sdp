package org.example.export;

import org.example.model.Summary;

import java.io.IOException;
import java.nio.file.Path;

public final class HtmlExporter implements ReportExporter {
    @Override
    public void export(Summary summary, Path path) throws IOException {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Event Studio — Summary</title>
                <style>body{font:16px system-ui;max-width:1100px;margin:40px auto;padding:20px}
                table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:10px;text-align:left}
                td{overflow-wrap:anywhere}th{background:#eef2f6}</style></head><body>
                """);
        html.append("<h1>").append(escape(summary.event().name())).append("</h1>")
                .append("<p>Event ID: ").append(escape(summary.event().id()))
                .append(" · Unit price (KZT): ").append(summary.event().unitPrice()).append("</p>")
                .append("<p>Pricing: ").append(escape(summary.pricing())).append("</p>")
                .append("<p>Capacity: ").append(summary.event().capacity())
                .append(" · Reserved: ").append(summary.reservedSeats())
                .append(" · Available: ").append(summary.availableSeats()).append("</p>")
                .append("<h2>Participants (customer names)</h2><ul>");
        summary.participants().forEach(name -> html.append("<li>").append(escape(name)).append("</li>"));
        html.append("</ul><h2>Bookings</h2><table><thead><tr><th>ID</th><th>Customer</th>")
                .append("<th>Seats</th><th>Status</th><th>Amount (KZT)</th></tr></thead><tbody>");
        for (Summary.BookingLine line : summary.bookings()) {
            var booking = line.booking();
            html.append("<tr><td>").append(escape(booking.id())).append("</td><td>")
                    .append(escape(booking.customerName())).append("</td><td>")
                    .append(booking.seatCount()).append("</td><td>").append(booking.status())
                    .append("</td><td>").append(line.price()).append("</td></tr>");
        }
        html.append("</tbody></table><h2>Total (KZT): ").append(summary.totalPrice()).append("</h2>")
                .append("<p>Cancelled bookings remain in history with zero amount. Prices are calculated ")
                .append("with the selected strategy; they are not stored quotes.</p></body></html>\n");
        ExportFiles.write(path, html.toString());
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
