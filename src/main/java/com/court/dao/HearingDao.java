package com.court.dao;

import com.court.model.Hearing;
import com.court.model.HearingStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HearingDao {

    Optional<Hearing> findById(int hearingId);

    /** All hearings on one day that are not cancelled (used for clash checks and cause lists). */
    List<Hearing> findByDate(LocalDate date);

    List<Hearing> findBetween(LocalDate from, LocalDate to, boolean includeCancelled);

    List<Hearing> findByCase(int caseId);

    List<Hearing> findForJudge(int judgeId, LocalDate from, LocalDate to);

    List<Hearing> findForLawyer(int lawyerId, LocalDate from, LocalDate to);

    /** The next scheduled hearing of a case, if any. */
    Optional<Hearing> findNextScheduled(int caseId);

    int insert(Connection con, int caseId, int judgeId, int courtroomId, LocalDate date, LocalTime time)
            throws SQLException;

    void updateStatus(Connection con, int hearingId, HearingStatus status) throws SQLException;

    void reschedule(Connection con, int hearingId, LocalDate date, LocalTime time) throws SQLException;

    /** Cancels every scheduled hearing of a case (used when a case is disposed). */
    void cancelScheduled(Connection con, int caseId) throws SQLException;
}
