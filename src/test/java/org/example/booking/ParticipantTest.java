package org.example.booking;

import org.example.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantTest {
    @Test
    void trimsAndKeepsFields() {
        Participant participant = new Participant("  Әсел ", " asel@example.kz ");

        assertEquals("Әсел", participant.name());
        assertEquals("asel@example.kz", participant.email());
        assertEquals("", new Participant("Дана").email());
    }

    @Test
    void rejectsBlankNameAndInvalidEmail() {
        ValidationException error = assertThrows(ValidationException.class, () -> new Participant(" ", "bad"));

        assertEquals(2, error.getErrors().size());
        assertThrows(ValidationException.class, () -> new Participant(null));
    }
}
