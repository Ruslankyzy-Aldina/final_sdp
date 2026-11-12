# Event models, Builder, Decorator, and validation

**Contributor:** Abilkhaiyr Akmaral (member A). This part adds event creation/editing, the participant model, optional service packages, and UI validation to the integrated Event Studio application.

## Architecture

| Layer | Main classes | Responsibility |
| --- | --- | --- |
| Swing UI | `BookingFrame`, `EventListPanel`, `PackagePanel`, `ErrorDialog` | Browse and edit events; configure packages; display errors |
| Application facade | `EventFacade` | Coordinate GUI actions with existing services |
| Domain | `Event`, `EventBuilder`, `Participant`, `ServicePackage` | Valid models and package composition |
| Booking and persistence | `BookingService`, `Repository`, `JsonStorage` | Capacity and status rules; save and reload events and bookings |

The event form parses text through `InputParser`, fills `EventBuilder`, and calls `EventFacade.createEvent(builder)`. The facade passes the resulting event to `BookingService`, which persists it. Errors are displayed by the shared GUI error dialog. The consolidated diagram of all six patterns is in the [main README](../README.md).

## Builder

`EventBuilder` collects an event's ID, name, date, location, capacity, and ticket price. `build()` checks all fields and returns an immutable `Event`. Multiple invalid fields are reported together through `ValidationException`. `EventBuilder.from(existing, clock)` supports editing while preserving an event's ID. The four-argument `Event` constructor remains available for older JSON records without date/location.

```java
Event event = new EventBuilder()
        .name(name)
        .date(date)
        .location(location)
        .capacity(capacity)
        .price(ticketPrice)
        .build();
```

Validation includes nonempty name/location, name length, a present date that is not in the past, capacity from 1 to 10,000, and a positive ticket price. Updating an event cannot reduce capacity below the seats already reserved. An event with any booking, including a cancelled booking, cannot be deleted.

## Decorator

`ServiceOption` declares cost and description operations. `BasePackage` provides the starting cost. `OptionDecorator` wraps another `ServiceOption` and adds its own cost and description. `CateringDecorator`, `PhotographerDecorator`, and `DecorDecorator` are concrete extras. `ServicePackage.configure(...)` builds a chain from the selected checkboxes.

```java
ServiceOption option = new BasePackage(baseCost);
if (catering)     option = new CateringDecorator(option);
if (photographer) option = new PhotographerDecorator(option);
if (decor)        option = new DecorDecorator(option);
BigDecimal total = option.getCost();
```

The three extras add 50,000, 30,000, and 20,000 KZT respectively. A 100,000 KZT base package with all three extras costs 200,000 KZT. The package panel previews the composition and remembers it for the selected event during the current application session.

## Scenarios and tests

| Scenario | Input and expected result | Recorded outcome |
| --- | --- | --- |
| Complete event journey | Create an event, configure a package, book and confirm seats, edit, restart, create and delete a second event | `EventCatalogScenarioTest.scenario1CreateConfigureBookEditAndDeleteSuccessfully` passed |
| Blank name | Submit a name containing only spaces; nothing is saved | `EventCatalogScenarioTest.scenario5EmptyEventNameIsRejectedWithMessageAndNothingIsSaved` passed |
| Invalid values | Use zero/negative capacity or price, a negative package base cost, or an invalid edit | `EventCatalogScenarioTest.scenario6ZeroOrNegativeCapacityOrPriceIsRejected` passed |
| Builder and Decorator units | Validate all Builder fields and combinations/order of extras | `EventBuilderTest` and `ServiceOptionDecoratorTest` cover these rules |

Other test classes cover `Participant`, `InputParser`, event CRUD and rollback, and the event/package screens on the Swing event dispatch thread. The full integrated project was tested on 12 November 2026 with JDK 25 and Maven 3.9.11: **59 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS** (`mvn clean test`).

## Known limitations

- Events and bookings are saved through `JsonStorage`; configured service packages are held in `EventFacade` memory and are not saved to `bookings.json`.
- Legacy events without date/location display placeholders. Those fields must be supplied when editing a legacy event.
- The GUI and validation messages are currently in Russian; this report and the repository documentation are in English.
