package com.court.service;

import com.court.exception.ScheduleConflictException;
import com.court.model.Hearing;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The scheduling rules of the court, written as pure functions (no database),
 * so they are easy to unit-test.
 * <p>
 * Clash detection uses a {@link HashSet} of "keys": for every hearing already booked on a day
 * we add {@code "J2@10:30"} (judge 2 busy at 10:30), {@code "R1@10:30"} (room 1) and
 * {@code "L6@10:30"} (lawyer 6). A new hearing clashes if any of its keys is already in the set -
 * a HashSet answers that in constant time.
 */
public final class ScheduleRules {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final LocalTime LUNCH_START = LocalTime.of(13, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);

    private ScheduleRules() {
    }

    /** All hearing slots in a day, e.g. 10:00, 10:30 ... 15:30, skipping the 1-2 pm lunch break. */
    public static List<LocalTime> allSlots(LocalTime start, LocalTime end, int minutes) {
        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime t = start; t.isBefore(end); t = t.plusMinutes(minutes)) {
            boolean lunch = !t.isBefore(LUNCH_START) && t.isBefore(LUNCH_END);
            if (!lunch) {
                slots.add(t);
            }
        }
        return slots;
    }

    /** The court must be open on the chosen day. */
    public static void checkDay(LocalDate date, LocalDate today, Map<LocalDate, String> holidays)
            throws ScheduleConflictException {
        if (date == null) {
            throw new ScheduleConflictException("Choose a hearing date.");
        }
        if (date.isBefore(today)) {
            throw new ScheduleConflictException("The hearing date cannot be in the past.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ScheduleConflictException(date.format(DATE) + " is a Sunday. The court is closed.");
        }
        if (holidays.containsKey(date)) {
            throw new ScheduleConflictException(date.format(DATE) + " is a court holiday ("
                    + holidays.get(date) + ").");
        }
    }

    /**
     * Throws if the judge, the room or the lawyer is already busy at that time,
     * or if the room is already full for the day.
     *
     * @param ignoreHearingId a hearing to leave out (when rescheduling it), or null
     */
    public static void checkClash(List<Hearing> sameDay, int judgeId, int roomId, int lawyerId, LocalTime time,
                                  Integer ignoreHearingId, int maxPerRoom) throws ScheduleConflictException {
        Set<String> booked = bookedKeys(sameDay, ignoreHearingId);

        if (booked.contains(judgeKey(judgeId, time))) {
            throw new ScheduleConflictException("The judge already has a hearing at " + time.format(TIME) + ".");
        }
        if (booked.contains(roomKey(roomId, time))) {
            throw new ScheduleConflictException("The courtroom is already booked at " + time.format(TIME) + ".");
        }
        if (booked.contains(lawyerKey(lawyerId, time))) {
            throw new ScheduleConflictException("The lawyer already has another hearing at " + time.format(TIME) + ".");
        }
        long inRoom = sameDay.stream()
                .filter(h -> h.courtroomId() == roomId)
                .filter(h -> ignoreHearingId == null || h.id() != ignoreHearingId)
                .count();
        if (inRoom >= maxPerRoom) {
            throw new ScheduleConflictException("This courtroom already has the maximum of " + maxPerRoom
                    + " hearings that day. Choose another date.");
        }
    }

    /** Slots in which the judge, room and lawyer are all free. */
    public static List<LocalTime> freeSlots(List<Hearing> sameDay, int judgeId, int roomId, int lawyerId,
                                            Integer ignoreHearingId, List<LocalTime> slots) {
        Set<String> booked = bookedKeys(sameDay, ignoreHearingId);
        List<LocalTime> free = new ArrayList<>();
        for (LocalTime t : slots) {
            if (!booked.contains(judgeKey(judgeId, t))
                    && !booked.contains(roomKey(roomId, t))
                    && !booked.contains(lawyerKey(lawyerId, t))) {
                free.add(t);
            }
        }
        return free;
    }

    private static Set<String> bookedKeys(List<Hearing> sameDay, Integer ignoreHearingId) {
        Set<String> keys = new HashSet<>();
        for (Hearing h : sameDay) {
            if (ignoreHearingId != null && h.id() == ignoreHearingId) continue;
            keys.add(judgeKey(h.judgeId(), h.time()));
            keys.add(roomKey(h.courtroomId(), h.time()));
            keys.add(lawyerKey(h.lawyerId(), h.time()));
        }
        return keys;
    }

    private static String judgeKey(int id, LocalTime t) {
        return "J" + id + "@" + t;
    }

    private static String roomKey(int id, LocalTime t) {
        return "R" + id + "@" + t;
    }

    private static String lawyerKey(int id, LocalTime t) {
        return "L" + id + "@" + t;
    }
}
