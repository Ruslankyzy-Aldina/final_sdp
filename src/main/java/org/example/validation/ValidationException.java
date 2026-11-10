package org.example.validation;

import java.util.List;

/**
 * Raised when user input or a business rule is violated.
 * Extends IllegalArgumentException so the existing GUI error handling shows it as-is.
 */
public class ValidationException extends IllegalArgumentException {
    private final List<String> errors;

    public ValidationException(String error) {
        this(List.of(error));
    }

    public ValidationException(List<String> errors) {
        super(String.join(System.lineSeparator(), requireErrors(errors)));
        this.errors = List.copyOf(errors);
    }

    /** All violated rules, in the order they were checked. */
    public List<String> getErrors() {
        return errors;
    }

    private static List<String> requireErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("At least one validation error is required");
        }
        return errors;
    }
}
