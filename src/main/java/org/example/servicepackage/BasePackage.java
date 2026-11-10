package org.example.servicepackage;

import org.example.validation.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Decorator concrete component: the base package (venue and organisation) without extras. */
public final class BasePackage implements ServiceOption {
    private final BigDecimal cost;

    public BasePackage(BigDecimal cost) {
        if (cost == null) {
            throw new ValidationException("Укажите стоимость базового пакета");
        }
        if (cost.signum() <= 0) {
            throw new ValidationException("Стоимость базового пакета должна быть больше нуля");
        }
        this.cost = cost.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Базовый пакет";
    }
}
