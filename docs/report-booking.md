# Booking, Strategy, Observer, and JSON storage

**Contributor:** Ruslankyzy Aldina (member B). This part provides the booking lifecycle, capacity checks, selectable pricing, event notifications, and persistence used by the integrated Event Studio GUI.

## Strategy

`PricingStrategy` declares `calculate(Booking)`. `RegularPricing` multiplies unit price by seats; `StudentDiscountPricing` reduces that amount by 10%; `GroupDiscountPricing` reduces it by 15% when the booking contains at least five seats. `PriceCalculator` stores the current strategy and changes it through `setStrategy`. The pricing choice is made in the interface and affects the current preview and summary; it is not written into the booking model.

```java
public interface PricingStrategy {
    BigDecimal calculate(Booking booking);
}

PriceCalculator calculator = new PriceCalculator(new RegularPricing());
calculator.setStrategy(new StudentDiscountPricing());
BigDecimal price = calculator.calculate(booking);
```

**Demonstration:** for one booking of five seats at 1,000.00 KZT per seat, regular, student, and group pricing produce 5,000.00, 4,500.00, and 4,250.00 KZT. `PriceCalculatorTest` checks the different prices, and `PricingOptionTest` checks selection through the UI option mapping.

## Observer

`BookingService` is the subject. It stores listeners in a `LinkedHashSet`, so subscribing the same listener instance twice does not duplicate notifications. `BookingListener` is the observer interface. `NotificationLogListener` records transitions, and `UiRefreshListener` refreshes the booking UI. Successful creation, confirmation, and cancellation notify listeners after the changed snapshot is saved. If persistence fails, the service rolls back and does not publish a false status event.

```java
public interface BookingListener {
    void onStatusChanged(Booking booking, BookingStatus oldStatus,
                         BookingStatus newStatus);
}

service.subscribe(new NotificationLogListener());
service.subscribe(new UiRefreshListener(this::refreshView));
service.confirmBooking(booking.id());
```

The important transitions are `null → CREATED`, `CREATED → CONFIRMED`, and `CREATED/CONFIRMED → CANCELLED`. A cancelled booking cannot be reopened. A repeated request for the current status is a no-op. `BookingServiceTest` checks the notification sequence.

## Booking rules and storage

`BookingService` creates bookings only for known events, requires a positive number of seats, and rejects a request that exceeds remaining capacity. Created and confirmed bookings occupy seats. A cancelled booking stays in history but releases its seats. The service also prevents an event from being reduced below its reserved seat count or deleted while any booking references it.

`Repository` separates storage from business logic. `JsonStorage` saves a `BookingSnapshot` of events and bookings to `data/bookings.json` and loads it at startup. It writes to a temporary file first and then replaces the target file. Service methods restore the previous in-memory state when saving fails. `JsonStorageTest` checks restart/reload behavior; `BookingPersistenceFailureTest` checks rollback on storage failure.

## Main sequence

1. The user chooses an event, a seat count, and a strategy in `BookingFrame`.
2. `EventFacade` previews the price and calls `BookingService.createBooking`.
3. `BookingService` checks capacity, stores the booking through `Repository`, and notifies subscribers with `CREATED`.
4. The user confirms the booking. The service saves `CONFIRMED` and notifies subscribers again.
5. The UI list and notification log refresh from their observer callbacks.

The consolidated class and sequence diagrams are in the [main README](../README.md).

## Automated scenarios

| Test | Input and expected result | Recorded outcome |
| --- | --- | --- |
| `PriceCalculatorTest.sameBookingHasThreePricesWhenStrategyChanges` | One five-seat booking produces three prices | Passed in the contributor test run |
| `PricingOptionTest.selectingEachOptionSwitchesThePrice` | UI choice changes the strategy and price | Passed in the contributor test run |
| `BookingServiceTest.creationConfirmationAndCancellationNotifySubscribers` | Three lifecycle changes notify subscribers | Passed in the contributor test run |
| `JsonStorageTest.restartRestoresEventsBookingsAndStatus` | Restart restores events, bookings, and statuses | Passed in the contributor test run |
| `BookingServiceTest.capacityIsReservedOnCreationAndReleasedOnCancellation` | Excess request fails; cancellation frees seats | Passed in the contributor test run |

The full integrated project was tested on 12 November 2026 with JDK 25 and Maven 3.9.11: **59 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Run `mvn clean test` to reproduce it. OpenAI Codex helped prepare the documentation.
