package org.example.servicepackage;

import java.math.BigDecimal;

/** Concrete decorator: Professional photographer. */
public final class PhotographerDecorator extends OptionDecorator {
    public static final BigDecimal DEFAULT_COST = new BigDecimal("30000.00");

    public PhotographerDecorator(ServiceOption wrapped) {
        this(wrapped, DEFAULT_COST);
    }

    public PhotographerDecorator(ServiceOption wrapped, BigDecimal cost) {
        super(wrapped, cost);
    }

    @Override
    protected String optionName() {
        return "Фотограф";
    }
}
