package org.example.servicepackage;

import java.math.BigDecimal;

/** Decorator component: anything that can be part of an event service package. */
public interface ServiceOption {
    BigDecimal getCost();

    String getDescription();
}
