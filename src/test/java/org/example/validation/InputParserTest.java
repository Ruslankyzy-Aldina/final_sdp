package org.example.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InputParserTest {
    @Test
    void parsesValidText() {
        assertEquals(-5, InputParser.parseInt(" -5 ", "Вместимость"));
        assertEquals(new BigDecimal("1500.50"), InputParser.parseMoney("1500,50", "Цена"));
        assertEquals(LocalDate.of(2026, 12, 1), InputParser.parseDate("2026-12-01", "Дата"));
    }

    @Test
    void reportsEmptyAndMalformedInputWithFieldName() {
        assertEquals("Вместимость: поле не может быть пустым",
                assertThrows(ValidationException.class, () -> InputParser.parseInt(" ", "Вместимость")).getMessage());
        assertEquals("Вместимость: введите целое число",
                assertThrows(ValidationException.class, () -> InputParser.parseInt("десять", "Вместимость")).getMessage());
        assertThrows(ValidationException.class, () -> InputParser.parseMoney("abc", "Цена"));
        assertEquals("Дата: используйте формат ГГГГ-ММ-ДД",
                assertThrows(ValidationException.class, () -> InputParser.parseDate("01.12.2026", "Дата")).getMessage());
    }
}
