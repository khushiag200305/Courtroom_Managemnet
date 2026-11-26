package com.court.dto;

import com.court.model.CaseStatus;
import com.court.model.CaseType;

import java.time.LocalDate;

/**
 * Short description of a case for lists and look-ups.
 *
 * @param date meaning depends on the list: filing date (scrutiny queue),
 *             next hearing date (public look-up), last order date (reserved list)
 * @param note extra text: lawyer name, defect reason, or room and judge
 */
public record CaseSummary(
        int id,
        String caseNumber,
        String title,
        CaseType type,
        CaseStatus status,
        LocalDate date,
        String note,
        boolean urgent) {
}
