package org.example.servicepackage;

import org.example.validation.ValidationException;

import java.math.BigDecimal;
import java.util.Objects;

/** The service package chosen for an event: a base package wrapped by the selected extras. */
public record ServicePackage(String eventId, ServiceOption option,
                             boolean catering, boolean photographer, boolean decor) {
    public ServicePackage {
        if (eventId == null || eventId.isBlank()) {
            throw new ValidationException("Выберите мероприятие для пакета");
        }
        Objects.requireNonNull(option, "option");
    }

    /** Builds the decorator chain base → catering → photographer → decor for the chosen extras. */
    public static ServicePackage configure(String eventId, BigDecimal baseCost,
                                           boolean catering, boolean photographer, boolean decor) {
        ServiceOption option = new BasePackage(baseCost);
        if (catering) {
            option = new CateringDecorator(option);
        }
        if (photographer) {
            option = new PhotographerDecorator(option);
        }
        if (decor) {
            option = new DecorDecorator(option);
        }
        return new ServicePackage(eventId, option, catering, photographer, decor);
    }

    public BigDecimal getCost() {
        return option.getCost();
    }

    public String getDescription() {
        return option.getDescription();
    }

    /** Cost of the base package alone, i.e. the innermost component of the chain. */
    public BigDecimal getBaseCost() {
        ServiceOption current = option;
        while (current instanceof OptionDecorator decorator) {
            current = decorator.getWrapped();
        }
        return current.getCost();
    }
}
