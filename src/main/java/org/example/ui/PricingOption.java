package org.example.ui;

import org.example.pricing.GroupDiscountPricing;
import org.example.pricing.PricingStrategy;
import org.example.pricing.RegularPricing;
import org.example.pricing.StudentDiscountPricing;

public enum PricingOption {
    REGULAR("Обычная", new RegularPricing()),
    STUDENT("Студенческая −10%", new StudentDiscountPricing()),
    GROUP("Групповая −15% (от 5 мест)", new GroupDiscountPricing());

    private final String label;
    private final PricingStrategy strategy;

    PricingOption(String label, PricingStrategy strategy) {
        this.label = label;
        this.strategy = strategy;
    }

    public PricingStrategy strategy() {
        return strategy;
    }

    @Override
    public String toString() {
        return label;
    }
}
