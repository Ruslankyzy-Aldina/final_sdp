# Factory Method, Facade, summary, and export

**Contributor:** Konys Ramazan (member C). This part adds the event summary, TXT/JSON/HTML export, and GUI integration with the existing booking, pricing, and storage services.

## Factory Method

`ExporterFactory` is the abstract creator. Its `export` operation calls the overridable `createExporter(format)` method and then delegates to the returned `ReportExporter` product. `DefaultExporterFactory` creates `TxtExporter`, `JsonExporter`, or `HtmlExporter` according to the selected format. Unknown formats are rejected. A test creator proves that the export operation invokes the overridden factory method.

```java
public abstract class ExporterFactory {
    public abstract ReportExporter createExporter(String format);

    public final void export(Summary summary, Path path, String format)
            throws IOException {
        createExporter(format).export(summary, path);
    }
}
```

The three exporters write UTF-8. JSON uses Jackson, and HTML escapes user text. Export writes a temporary file beside the chosen destination and then replaces the target. The GUI asks before overwriting and performs file writing in the background so the window remains responsive.

## Facade

`EventFacade` gives the GUI one API for booking, event management, pricing, package configuration, summary creation, and export. It delegates booking rules to the original `BookingService`; capacity checks, status persistence, and Observer notifications remain there. `BookingFrame` uses the facade without introducing a second application entry point.

```java
public Booking addBooking(String eventId, String customer, int seats) {
    Participant participant = new Participant(customer);
    previewPrice(eventId, participant.name(), seats);
    return service.createBooking(eventId, participant.name(), seats);
}

public Summary getSummary(String eventId) {
    return Summary.of(service.findEvent(eventId), service.bookings(),
                      calculator, pricing);
}
```

`Summary` is an immutable snapshot of the selected event. It lists unique customer-name strings, booking lines, reserved and available seats, and the price total under the current strategy. Cancelled bookings remain visible but count as zero seats and zero price. If group pricing cannot calculate an active booking with fewer than five seats, the summary shows an error and export is disabled; no hidden fallback price is used.

## Scenarios and recorded tests

| Scenario | Input and expected result | Recorded outcome |
| --- | --- | --- |
| Export formats | Eight-seat concert at 1,000 KZT; five-seat active booking; two-seat cancelled booking; student pricing; export TXT/JSON/HTML | `EventFacadeTest.scenario7ExportsSelectedFormatWithCorrectTotalsAndUnicode` passed; 4,500.00 total, five occupied and three free seats |
| Over-capacity boundary | Fill all seats and request one more; then cancel the first booking | `EventFacadeTest.scenario8RejectsOverCapacityWithoutChangingStorageOrObservers` passed; excess request leaves data and notifications unchanged |
| Exporter selection | Request three formats and an unsupported format | `ExporterFactoryTest` covers product selection and error handling |
| GUI integration | Change pricing and booking status in the original window | `BookingFrameIntegrationTest` covers this on a graphical Swing environment |

An earlier `mvn clean verify` run on 10 November 2026 reported 33 passing tests for the booking/export integration. The **full integrated project** was tested on 12 November 2026 with JDK 25 and Maven 3.9.11: **59 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS** (`mvn clean test`). `BookingFrameIntegrationTest` requires a graphical environment and is skipped in headless environments. The GUI file chooser was not exercised by an automated test.

## Limitations

- The selected pricing strategy is a view setting, not a price stored with each booking.
- Participants in the summary are unique name strings; identical names do not represent separate identities.
- The summary includes the selected event only, regardless of the selected row in the overall booking table.
- The GUI export uses the immutable summary snapshot visible when the export button is pressed.

See the [main README](../README.md) for the consolidated diagrams, pattern table, and run instructions.
