package com.court.dao;

import com.court.model.CaseStatus;
import com.court.model.CaseType;
import com.court.model.CourtCase;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Case queries. Methods that take a {@link Connection} run inside a transaction
 * started by a service (see {@code DBConnection.inTransaction}).
 */
public interface CaseDao extends GenericDao<CourtCase, Integer> {

    List<CourtCase> findByStatus(CaseStatus status);

    List<CourtCase> findByLawyer(int lawyerId);

    List<CourtCase> findByJudge(int judgeId);

    /** Registered, pending and reserved cases. */
    List<CourtCase> findActive();

    /** The case with its parties loaded. */
    Optional<CourtCase> findWithParties(int caseId);

    /** Existing case numbers of one type and year, e.g. all "CS/.../2026". */
    List<String> caseNumbersFor(CaseType type, int year);

    int insert(Connection con, CourtCase courtCase) throws SQLException;

    /** Saves a corrected filing and sends it back to the scrutiny queue. */
    void updateFiling(Connection con, CourtCase courtCase) throws SQLException;

    void register(Connection con, int caseId, String caseNumber, int judgeId, int courtroomId,
                  LocalDate registrationDate, LocalDate firstHearing) throws SQLException;

    void markDefective(int caseId, String reason);

    void updateStatus(Connection con, int caseId, CaseStatus status, LocalDate nextHearing) throws SQLException;

    void setNextHearing(Connection con, int caseId, LocalDate nextHearing) throws SQLException;

    void dispose(Connection con, int caseId, String judgment, LocalDate disposalDate) throws SQLException;
}
