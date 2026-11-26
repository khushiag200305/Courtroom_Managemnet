package com.court.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One hearing of a case: which judge, which room, which date and slot.
 */
public record Hearing(
        int id,
        int caseId,
        String caseNumber,
        String caseTitle,
        CaseStatus caseStatus,
        int lawyerId,
        int judgeId,
        String judgeName,
        int courtroomId,
        String roomNo,
        LocalDate date,
        LocalTime time,
        HearingStatus status) {

    public LocalDateTime startsAt() {
        return LocalDateTime.of(date, time);
    }

    public boolean isUpcoming() {
        return status == HearingStatus.SCHEDULED && !date.isBefore(LocalDate.now());
    }
}
