package com.court.model;

import java.time.LocalDate;

/**
 * An order passed by a judge at a hearing (named CourtOrder to avoid confusion
 * with SQL's ORDER BY).
 */
public record CourtOrder(
        int id,
        int caseId,
        String caseNumber,
        String caseTitle,
        Integer hearingId,
        int judgeId,
        String judgeName,
        String text,
        OrderOutcome outcome,
        LocalDate nextDate,
        LocalDate orderDate) {
}
