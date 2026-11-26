package com.court.dao.impl;

import com.court.dao.CaseDao;
import com.court.dao.PartyDao;
import com.court.model.CaseStatus;
import com.court.model.CaseType;
import com.court.model.CourtCase;
import com.court.util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class CaseDaoImpl extends JdbcSupport implements CaseDao {

    private static final String SELECT = """
            SELECT c.*, l.name AS lawyer_name, j.name AS judge_name, r.room_no
            FROM cases c
            JOIN users l           ON l.id = c.filed_by
            LEFT JOIN users j      ON j.id = c.judge_id
            LEFT JOIN courtrooms r ON r.id = c.courtroom_id
            """;

    private final PartyDao partyDao = new PartyDaoImpl();

    // ------------------------------------------------------------------ GenericDao

    @Override
    public CourtCase save(CourtCase courtCase) {
        int id = DBConnection.inTransaction(con -> {
            int newId = insert(con, courtCase);
            partyDao.insertAll(con, newId, courtCase.getParties());
            return newId;
        });
        courtCase.setId(id);
        return courtCase;
    }

    @Override
    public Optional<CourtCase> findById(Integer id) {
        return queryOne(SELECT + " WHERE c.id = ?", this::map, id);
    }

    @Override
    public List<CourtCase> findAll() {
        return query(SELECT + " ORDER BY c.filing_date DESC", this::map);
    }

    @Override
    public boolean update(CourtCase c) {
        return update("UPDATE cases SET title = ?, case_type = ?, description = ?, urgent = ? WHERE id = ?",
                c.getTitle(), c.getType(), c.getDescription(), c.isUrgent(), c.getId()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return update("DELETE FROM cases WHERE id = ?", id) > 0;
    }

    // ------------------------------------------------------------------ finders

    @Override
    public List<CourtCase> findByStatus(CaseStatus status) {
        return query(SELECT + " WHERE c.status = ? ORDER BY c.urgent DESC, c.filing_date", this::map, status);
    }

    @Override
    public List<CourtCase> findByLawyer(int lawyerId) {
        return query(SELECT + " WHERE c.filed_by = ? ORDER BY c.filing_date DESC", this::map, lawyerId);
    }

    @Override
    public List<CourtCase> findByJudge(int judgeId) {
        return query(SELECT + " WHERE c.judge_id = ? ORDER BY c.next_hearing_date IS NULL, c.next_hearing_date",
                this::map, judgeId);
    }

    @Override
    public List<CourtCase> findActive() {
        return query(SELECT + " WHERE c.status IN ('REGISTERED','PENDING','RESERVED') ORDER BY c.case_number",
                this::map);
    }

    @Override
    public Optional<CourtCase> findWithParties(int caseId) {
        Optional<CourtCase> found = findById(caseId);
        found.ifPresent(c -> c.getParties().addAll(partyDao.findByCase(caseId)));
        return found;
    }

    @Override
    public List<String> caseNumbersFor(CaseType type, int year) {
        return query("SELECT case_number FROM cases WHERE case_number LIKE ?",
                rs -> rs.getString(1), type.getPrefix() + "/%/" + year);
    }

    // ------------------------------------------------------------------ writes inside transactions

    @Override
    public int insert(Connection con, CourtCase c) throws SQLException {
        return insert(con, """
                INSERT INTO cases (title, case_type, description, status, urgent, filed_by, filing_date)
                VALUES (?, ?, ?, 'FILED', ?, ?, ?)
                """, c.getTitle(), c.getType(), c.getDescription(), c.isUrgent(), c.getFiledById(), c.getFilingDate());
    }

    @Override
    public void updateFiling(Connection con, CourtCase c) throws SQLException {
        update(con, """
                UPDATE cases SET title = ?, case_type = ?, description = ?, urgent = ?,
                       status = 'FILED', defect_reason = NULL, filing_date = ?
                WHERE id = ?
                """, c.getTitle(), c.getType(), c.getDescription(), c.isUrgent(), LocalDate.now(), c.getId());
    }

    @Override
    public void register(Connection con, int caseId, String caseNumber, int judgeId, int courtroomId,
                         LocalDate registrationDate, LocalDate firstHearing) throws SQLException {
        update(con, """
                UPDATE cases SET case_number = ?, status = 'REGISTERED', judge_id = ?, courtroom_id = ?,
                       registration_date = ?, next_hearing_date = ?
                WHERE id = ?
                """, caseNumber, judgeId, courtroomId, registrationDate, firstHearing, caseId);
    }

    @Override
    public void markDefective(int caseId, String reason) {
        update("UPDATE cases SET status = 'DEFECTIVE', defect_reason = ? WHERE id = ?", reason, caseId);
    }

    @Override
    public void updateStatus(Connection con, int caseId, CaseStatus status, LocalDate nextHearing) throws SQLException {
        update(con, "UPDATE cases SET status = ?, next_hearing_date = ? WHERE id = ?", status, nextHearing, caseId);
    }

    @Override
    public void setNextHearing(Connection con, int caseId, LocalDate nextHearing) throws SQLException {
        update(con, "UPDATE cases SET next_hearing_date = ? WHERE id = ?", nextHearing, caseId);
    }

    @Override
    public void dispose(Connection con, int caseId, String judgment, LocalDate disposalDate) throws SQLException {
        update(con, """
                UPDATE cases SET status = 'DISPOSED', judgment = ?, disposal_date = ?, next_hearing_date = NULL
                WHERE id = ?
                """, judgment, disposalDate, caseId);
    }

    // ------------------------------------------------------------------ mapping

    private CourtCase map(ResultSet rs) throws SQLException {
        CourtCase c = new CourtCase();
        c.setId(rs.getInt("id"));
        c.setCaseNumber(rs.getString("case_number"));
        c.setTitle(rs.getString("title"));
        c.setType(CaseType.valueOf(rs.getString("case_type")));
        c.setDescription(rs.getString("description"));
        c.setStatusFromDatabase(CaseStatus.valueOf(rs.getString("status")));
        c.setUrgent(rs.getBoolean("urgent"));
        c.setFiledById(rs.getInt("filed_by"));
        c.setFiledByName(rs.getString("lawyer_name"));
        c.setJudgeId(nullableInt(rs, "judge_id"));
        c.setJudgeName(rs.getString("judge_name"));
        c.setCourtroomId(nullableInt(rs, "courtroom_id"));
        c.setRoomNo(rs.getString("room_no"));
        c.setFilingDate(date(rs, "filing_date"));
        c.setRegistrationDate(date(rs, "registration_date"));
        c.setNextHearingDate(date(rs, "next_hearing_date"));
        c.setDisposalDate(date(rs, "disposal_date"));
        c.setDefectReason(rs.getString("defect_reason"));
        c.setJudgment(rs.getString("judgment"));
        return c;
    }
}
