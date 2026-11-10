package org.example.servicepackage;

import java.math.BigDecimal;

/** Concrete decorator: Catering for guests. */
public final class CateringDecorator extends OptionDecorator {
    public static final BigDecimal DEFAULT_COST = new BigDecimal("50000.00");

    public CateringDecorator(ServiceOption wrapped) {
        this(wrapped, DEFAULT_COST);
    }

    public CateringDecorator(ServiceOption wrapped, BigDecimal cost) {
        super(wrapped, cost);
    }

    @Override
    protected String optionName() {
        return "Кейтеринг";
    }
}
