package org.example.booking;

import org.example.validation.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A person attending an event. Bookings store the participant's name ({@link Booking#customerName()});
 * the email is optional contact data.
 */
public record Participant(String name, String email) {
    public static final int MAX_NAME_LENGTH = 100;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Participant {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim();
        List<String> errors = new ArrayList<>();
        if (name.isEmpty()) {
            errors.add("Имя участника не может быть пустым");
        } else if (name.length() > MAX_NAME_LENGTH) {
            errors.add("Имя участника не может быть длиннее " + MAX_NAME_LENGTH + " символов");
        }
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            errors.add("Некорректный email участника");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public Participant(String name) {
        this(name, "");
    }
}
