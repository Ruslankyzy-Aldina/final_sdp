package org.example.servicepackage;

import org.example.validation.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Decorator: wraps another {@link ServiceOption} and adds its own cost and description.
 * Subclasses only name the extra service and its price.
 */
public abstract class OptionDecorator implements ServiceOption {
    private final ServiceOption wrapped;
    private final BigDecimal extraCost;

    protected OptionDecorator(ServiceOption wrapped, BigDecimal extraCost) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
        if (extraCost == null || extraCost.signum() < 0) {
            throw new ValidationException("Стоимость услуги «" + optionName() + "» не может быть отрицательной");
        }
        this.extraCost = extraCost.setScale(2, RoundingMode.HALF_UP);
    }

    /** Human-readable name of the added service. */
    protected abstract String optionName();

    public ServiceOption getWrapped() {
        return wrapped;
    }

    public BigDecimal getExtraCost() {
        return extraCost;
    }

    @Override
    public BigDecimal getCost() {
        return wrapped.getCost().add(extraCost);
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + " + optionName();
    }
}
