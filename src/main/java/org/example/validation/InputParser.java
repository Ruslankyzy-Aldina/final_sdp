package org.example.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Converts raw text from form fields into typed values with readable error messages. */
public final class InputParser {
    private InputParser() {
    }

    public static int parseInt(String text, String field) {
        String value = requireText(text, field);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException(field + ": введите целое число");
        }
    }

    public static BigDecimal parseMoney(String text, String field) {
        String value = requireText(text, field).replace(',', '.');
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException(field + ": введите число, например 1500.00");
        }
    }

    public static LocalDate parseDate(String text, String field) {
        String value = requireText(text, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ValidationException(field + ": используйте формат ГГГГ-ММ-ДД");
        }
    }

    private static String requireText(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(field + ": поле не может быть пустым");
        }
        return text.trim();
    }
}
