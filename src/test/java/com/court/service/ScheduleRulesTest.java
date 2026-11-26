package com.court.service;

import com.court.exception.ScheduleConflictException;
import com.court.model.CaseStatus;
import com.court.model.Hearing;
import com.court.model.HearingStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleRulesTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 30);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    /** A scheduled hearing for judge 2 in room 1, filed by lawyer 6. */
    private static Hearing hearing(int id, int judge, int room, int lawyer, LocalTime time) {
        return new Hearing(id, 100 + id, "CS/10" + id + "/2026", "A vs. B", CaseStatus.PENDING, lawyer, judge,
                "Justice X", room, String.valueOf(room), MONDAY, time, HearingStatus.SCHEDULED);
    }

    @Test
    void slotsSkipTheLunchBreak() {
        List<LocalTime> slots = ScheduleRules.allSlots(TEN, LocalTime.of(16, 0), 30);
        assertEquals(10, slots.size());                     // 10:00-12:30 (6) + 14:00-15:30 (4)
        assertFalse(slots.contains(LocalTime.of(13, 0)));
        assertFalse(slots.contains(LocalTime.of(13, 30)));
        assertTrue(slots.contains(LocalTime.of(14, 0)));
    }

    @Test
    void sundayIsRejected() {
        LocalDate sunday = LocalDate.of(2026, 11, 29);
        assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkDay(sunday, MONDAY.minusDays(7), Map.of()));
    }

    @Test
    void holidayIsRejected() {
        LocalDate christmas = LocalDate.of(2026, 12, 25);
        assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkDay(christmas, MONDAY, Map.of(christmas, "Christmas")));
    }

    @Test
    void pastDateIsRejected() {
        assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkDay(MONDAY.minusDays(1), MONDAY, Map.of()));
    }

    @Test
    void judgeCannotBeDoubleBooked() {
        List<Hearing> day = List.of(hearing(1, 2, 1, 6, TEN));
        ScheduleConflictException e = assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkClash(day, 2, 3, 7, TEN, null, 12));
        assertTrue(e.getMessage().contains("judge"));
    }

    @Test
    void courtroomCannotBeDoubleBooked() {
        List<Hearing> day = List.of(hearing(1, 2, 1, 6, TEN));
        ScheduleConflictException e = assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkClash(day, 3, 1, 7, TEN, null, 12));
        assertTrue(e.getMessage().contains("courtroom"));
    }

    @Test
    void lawyerCannotBeInTwoCourtsAtOnce() {
        List<Hearing> day = List.of(hearing(1, 2, 1, 6, TEN));
        ScheduleConflictException e = assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkClash(day, 3, 2, 6, TEN, null, 12));
        assertTrue(e.getMessage().contains("lawyer"));
    }

    @Test
    void reschedulingIgnoresTheHearingItself() throws ScheduleConflictException {
        List<Hearing> day = List.of(hearing(1, 2, 1, 6, TEN));
        ScheduleRules.checkClash(day, 2, 1, 6, TEN, 1, 12);   // no exception expected
    }

    @Test
    void fullCourtroomIsRejected() {
        List<Hearing> day = List.of(hearing(1, 2, 1, 6, TEN), hearing(2, 2, 1, 7, LocalTime.of(10, 30)));
        assertThrows(ScheduleConflictException.class,
                () -> ScheduleRules.checkClash(day, 2, 1, 8, LocalTime.of(11, 0), null, 2));
    }

    @Test
    void freeSlotsLeaveOutBusyTimes() {
        List<LocalTime> slots = List.of(TEN, LocalTime.of(10, 30), LocalTime.of(11, 0));
        List<Hearing> day = List.of(hearing(1, 2, 1, 6, TEN), hearing(2, 9, 4, 7, LocalTime.of(11, 0)));
        // judge 2 is busy at 10:00, lawyer 7 at 11:00 -> only 10:30 is free for judge 2 + lawyer 7
        assertEquals(List.of(LocalTime.of(10, 30)), ScheduleRules.freeSlots(day, 2, 1, 7, null, slots));
    }
}
