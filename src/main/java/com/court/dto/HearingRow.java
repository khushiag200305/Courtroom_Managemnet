package com.court.dto;

import com.court.model.CaseStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One line of a hearing list (today's hearings, cause list, upcoming hearings).
 * A record is a short, immutable data class: Java writes the constructor,
 * getters, equals, hashCode and toString automatically.
 */
public record HearingRow(
        LocalDate date,
        LocalTime time,
        String caseNumber,
        String title,
        String roomNo,
        String judgeName,
        CaseStatus status,
        boolean urgent) {
}
