package org.example.servicepackage;

import org.example.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ServiceOptionDecoratorTest {
    private static final BigDecimal BASE = new BigDecimal("100000");

    @Test
    void basePackageAlone() {
        ServiceOption option = new BasePackage(BASE);

        assertEquals(new BigDecimal("100000.00"), option.getCost());
        assertEquals("Базовый пакет", option.getDescription());
    }

    @Test
    void priceWithSeveralOptionsIsTheSumOfAllLayers() {
        ServiceOption option = new DecorDecorator(
                new PhotographerDecorator(new CateringDecorator(new BasePackage(BASE))));

        // 100000 + 50000 + 30000 + 20000
        assertEquals(new BigDecimal("200000.00"), option.getCost());
        assertEquals("Базовый пакет + Кейтеринг + Фотограф + Декор", option.getDescription());
    }

    @Test
    void costDoesNotDependOnOrderAndCustomPricesAreSupported() {
        ServiceOption a = new CateringDecorator(new PhotographerDecorator(new BasePackage(BASE)));
        ServiceOption b = new PhotographerDecorator(new CateringDecorator(new BasePackage(BASE)));
        assertEquals(a.getCost(), b.getCost());

        ServiceOption custom = new CateringDecorator(new BasePackage(BASE), new BigDecimal("12500.50"));
        assertEquals(new BigDecimal("112500.50"), custom.getCost());
    }

    @Test
    void servicePackageBuildsChainFromSelectedExtras() {
        ServicePackage servicePackage = ServicePackage.configure("concert", BASE, true, false, true);

        assertEquals(new BigDecimal("170000.00"), servicePackage.getCost());
        assertEquals("Базовый пакет + Кейтеринг + Декор", servicePackage.getDescription());
        assertEquals(new BigDecimal("100000.00"), servicePackage.getBaseCost());
        assertTrue(servicePackage.catering());
        assertFalse(servicePackage.photographer());
        assertTrue(servicePackage.decor());
    }

    @Test
    void rejectsZeroOrNegativePrices() {
        assertEquals("Стоимость базового пакета должна быть больше нуля",
                assertThrows(ValidationException.class, () -> new BasePackage(BigDecimal.ZERO)).getMessage());
        assertThrows(ValidationException.class, () -> new BasePackage(new BigDecimal("-1")));
        assertThrows(ValidationException.class, () -> new BasePackage(null));
        assertThrows(ValidationException.class,
                () -> new DecorDecorator(new BasePackage(BASE), new BigDecimal("-0.01")));
    }
}
