package com.court.dao.impl;

import com.court.dao.HearingDao;
import com.court.model.CaseStatus;
import com.court.model.Hearing;
import com.court.model.HearingStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class HearingDaoImpl extends JdbcSupport implements HearingDao {

    private static final String SELECT = """
            SELECT h.*, c.case_number, c.title, c.status AS case_status, c.filed_by,
                   u.name AS judge_name, r.room_no
            FROM hearings h
            JOIN cases c      ON c.id = h.case_id
            JOIN users u      ON u.id = h.judge_id
            JOIN courtrooms r ON r.id = h.courtroom_id
            """;

    private static final String ORDER = " ORDER BY h.hearing_date, h.slot_time, r.room_no";

    @Override
    public Optional<Hearing> findById(int hearingId) {
        return queryOne(SELECT + " WHERE h.id = ?", this::map, hearingId);
    }

    @Override
    public List<Hearing> findByDate(LocalDate date) {
        return query(SELECT + " WHERE h.hearing_date = ? AND h.status <> 'CANCELLED'" + ORDER, this::map, date);
    }

    @Override
    public List<Hearing> findBetween(LocalDate from, LocalDate to, boolean includeCancelled) {
        String filter = includeCancelled ? "" : " AND h.status <> 'CANCELLED'";
        return query(SELECT + " WHERE h.hearing_date BETWEEN ? AND ?" + filter + ORDER, this::map, from, to);
    }

    @Override
    public List<Hearing> findByCase(int caseId) {
        return query(SELECT + " WHERE h.case_id = ?" + ORDER, this::map, caseId);
    }

    @Override
    public List<Hearing> findForJudge(int judgeId, LocalDate from, LocalDate to) {
        return query(SELECT + " WHERE h.judge_id = ? AND h.status <> 'CANCELLED' AND h.hearing_date BETWEEN ? AND ?"
                + ORDER, this::map, judgeId, from, to);
    }

    @Override
    public List<Hearing> findForLawyer(int lawyerId, LocalDate from, LocalDate to) {
        return query(SELECT + " WHERE c.filed_by = ? AND h.status <> 'CANCELLED' AND h.hearing_date BETWEEN ? AND ?"
                + ORDER, this::map, lawyerId, from, to);
    }

    @Override
    public Optional<Hearing> findNextScheduled(int caseId) {
        return queryOne(SELECT + " WHERE h.case_id = ? AND h.status = 'SCHEDULED'" + ORDER + " LIMIT 1",
                this::map, caseId);
    }

    @Override
    public int insert(Connection con, int caseId, int judgeId, int courtroomId, LocalDate date, LocalTime time)
            throws SQLException {
        return insert(con, """
                INSERT INTO hearings (case_id, judge_id, courtroom_id, hearing_date, slot_time, status)
                VALUES (?, ?, ?, ?, ?, 'SCHEDULED')
                """, caseId, judgeId, courtroomId, date, time);
    }

    @Override
    public void updateStatus(Connection con, int hearingId, HearingStatus status) throws SQLException {
        update(con, "UPDATE hearings SET status = ? WHERE id = ?", status, hearingId);
    }

    @Override
    public void reschedule(Connection con, int hearingId, LocalDate date, LocalTime time) throws SQLException {
        update(con, "UPDATE hearings SET hearing_date = ?, slot_time = ? WHERE id = ?", date, time, hearingId);
    }

    @Override
    public void cancelScheduled(Connection con, int caseId) throws SQLException {
        update(con, "UPDATE hearings SET status = 'CANCELLED' WHERE case_id = ? AND status = 'SCHEDULED'", caseId);
    }

    private Hearing map(ResultSet rs) throws SQLException {
        return new Hearing(
                rs.getInt("id"),
                rs.getInt("case_id"),
                rs.getString("case_number"),
                rs.getString("title"),
                CaseStatus.valueOf(rs.getString("case_status")),
                rs.getInt("filed_by"),
                rs.getInt("judge_id"),
                rs.getString("judge_name"),
                rs.getInt("courtroom_id"),
                rs.getString("room_no"),
                date(rs, "hearing_date"),
                time(rs, "slot_time"),
                HearingStatus.valueOf(rs.getString("status")));
    }
}
