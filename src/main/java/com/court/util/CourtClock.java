package com.court.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Works out whether the court is sitting right now, using the hearing hours in app.properties.
 * Courts sit Monday to Saturday; Sunday is closed.
 */
public final class CourtClock {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");

    private CourtClock() {
    }

    public static LocalTime opens() {
        return AppConfig.getTime("hearing.day.start", LocalTime.of(10, 0));
    }

    public static LocalTime closes() {
        return AppConfig.getTime("hearing.day.end", LocalTime.of(16, 0));
    }

    public static boolean isInSession(LocalDateTime now) {
        if (now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(opens()) && time.isBefore(closes());
    }

    /** e.g. "Court in session until 4:00 pm" or "Court opens at 10:00 am". */
    public static String status(LocalDateTime now) {
        if (isInSession(now)) {
            return "Court in session until " + closes().format(TIME).toLowerCase();
        }
        if (now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "Court closed on Sundays";
        }
        if (now.toLocalTime().isBefore(opens())) {
            return "Court opens at " + opens().format(TIME).toLowerCase();
        }
        return "Court has risen for the day";
    }
}
