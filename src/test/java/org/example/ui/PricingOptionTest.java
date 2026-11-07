package org.example.ui;

import org.example.booking.Booking;
import org.example.booking.BookingStatus;
import org.example.pricing.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingOptionTest {
    @Test
    void selectingEachOptionSwitchesThePrice() {
        Booking booking = new Booking("booking-1", "event-1", "Aida", 5,
                new BigDecimal("100.00"), BookingStatus.CREATED);
        PriceCalculator calculator = new PriceCalculator(PricingOption.REGULAR.strategy());

        assertEquals(new BigDecimal("500.00"), calculator.calculate(booking));
        calculator.setStrategy(PricingOption.STUDENT.strategy());
        assertEquals(new BigDecimal("450.00"), calculator.calculate(booking));
        calculator.setStrategy(PricingOption.GROUP.strategy());
        assertEquals(new BigDecimal("425.00"), calculator.calculate(booking));
    }
}
