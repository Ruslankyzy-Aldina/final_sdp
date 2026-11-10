package org.example.servicepackage;

import java.math.BigDecimal;

/** Concrete decorator: Hall decoration. */
public final class DecorDecorator extends OptionDecorator {
    public static final BigDecimal DEFAULT_COST = new BigDecimal("20000.00");

    public DecorDecorator(ServiceOption wrapped) {
        this(wrapped, DEFAULT_COST);
    }

    public DecorDecorator(ServiceOption wrapped, BigDecimal cost) {
        super(wrapped, cost);
    }

    @Override
    protected String optionName() {
        return "Декор";
    }
}
