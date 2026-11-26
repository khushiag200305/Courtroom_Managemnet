package com.court.util;

import com.court.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilTest {

    @Test
    void passwordHashVerifiesOnlyTheRightPassword() {
        String hash = PasswordUtil.hash("judge123");
        assertTrue(PasswordUtil.verify("judge123", hash));
        assertFalse(PasswordUtil.verify("Judge123", hash));
        assertFalse(PasswordUtil.verify("judge123", "not-a-hash"));
    }

    @Test
    void samePasswordGetsADifferentSaltEachTime() {
        assertNotEquals(PasswordUtil.hash("admin123"), PasswordUtil.hash("admin123"));
    }

    @Test
    void phoneNumbersAreCleanedAndChecked() throws ValidationException {
        assertEquals("9876543210", Validators.phone("+91 98765-43210"));
        assertNull(Validators.phone("  "));
        assertThrows(ValidationException.class, () -> Validators.phone("12345"));
        assertThrows(ValidationException.class, () -> Validators.phone("5876543210"));
    }

    @Test
    void barIdMustFollowTheStateNumberYearFormat() throws ValidationException {
        assertEquals("PB/1021/2014", Validators.barId("pb/1021/2014"));
        assertThrows(ValidationException.class, () -> Validators.barId("1021-2014"));
    }

    @Test
    void usernamesAndEmailsAreChecked() throws ValidationException {
        assertEquals("judge5", Validators.username("Judge5"));
        assertThrows(ValidationException.class, () -> Validators.username("5judge"));
        assertThrows(ValidationException.class, () -> Validators.email("not an email"));
        assertNull(Validators.email(""));
    }

    @Test
    void requiredTextMustNotBeEmptyOrTooLong() {
        assertThrows(ValidationException.class, () -> Validators.required("   ", "Name", 10));
        assertThrows(ValidationException.class, () -> Validators.required("x".repeat(11), "Name", 10));
    }

    @Test
    void courtSitsOnWeekdaysDuringHearingHours() {
        LocalDateTime mondayMorning = LocalDateTime.of(2026, 11, 30, 11, 0);
        LocalDateTime mondayEvening = LocalDateTime.of(2026, 11, 30, 18, 0);
        LocalDateTime sunday = LocalDateTime.of(2026, 11, 29, 11, 0);
        assertTrue(CourtClock.isInSession(mondayMorning));
        assertFalse(CourtClock.isInSession(mondayEvening));
        assertFalse(CourtClock.isInSession(sunday));
        assertTrue(CourtClock.status(sunday).contains("Sunday"));
    }
}
