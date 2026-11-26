package com.court.dao.impl;

import com.court.dao.DashboardDao;
import com.court.dto.CaseSummary;
import com.court.dto.CourtroomLoad;
import com.court.dto.DashboardStats;
import com.court.dto.HearingRow;
import com.court.exception.DataAccessException;
import com.court.model.CaseStatus;
import com.court.model.CaseType;
import com.court.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * JDBC implementation of {@link DashboardDao}. Every query uses PreparedStatement.
 */
public class DashboardDaoImpl implements DashboardDao {

    private static final String ACTIVE = "('REGISTERED','PENDING','RESERVED')";

    /** Sort order for hearing lists: by time, then by room number. */
    private static final Comparator<HearingRow> BY_DATE_TIME_ROOM =
            Comparator.comparing(HearingRow::date)
                    .thenComparing(HearingRow::time)
                    .thenComparing(HearingRow::roomNo);

    private static final String HEARING_SELECT = """
            SELECT h.hearing_date, h.slot_time, c.case_number, c.title, c.status, c.urgent,
                   r.room_no, u.name AS judge_name
            FROM hearings h
            JOIN cases c      ON c.id = h.case_id
            JOIN courtrooms r ON r.id = h.courtroom_id
            JOIN users u      ON u.id = h.judge_id
            WHERE h.status <> 'CANCELLED'
            """;

    // ================================================================ Registrar

    @Override
    public DashboardStats.Registrar registrarStats() {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM cases WHERE status IN %1$s) AS pending,
                  (SELECT COUNT(*) FROM cases WHERE filing_date >= CURDATE() - INTERVAL 7 DAY) AS filed_week,
                  (SELECT COUNT(*) FROM hearings WHERE hearing_date = CURDATE() AND status <> 'CANCELLED') AS hearings_today,
                  (SELECT COUNT(DISTINCT courtroom_id) FROM hearings
                     WHERE hearing_date = CURDATE() AND status <> 'CANCELLED') AS rooms_today,
                  (SELECT COUNT(*) FROM cases WHERE status = 'FILED') AS scrutiny,
                  (SELECT COUNT(*) FROM cases WHERE status = 'FILED'
                     AND filing_date < CURDATE() - INTERVAL 3 DAY) AS scrutiny_overdue,
                  (SELECT COUNT(*) FROM cases WHERE status = 'DISPOSED'
                     AND disposal_date >= CURDATE() - INTERVAL 30 DAY) AS disposed_30,
                  (SELECT COUNT(*) FROM cases WHERE status = 'DISPOSED') AS disposed_all,
                  (SELECT COUNT(*) FROM cases WHERE case_number IS NOT NULL) AS registered_all
                """.formatted(ACTIVE);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int disposedAll = rs.getInt("disposed_all");
            int registeredAll = rs.getInt("registered_all");
            // Math.round turns e.g. 18.6% into 19%; guard against dividing by zero
            int rate = registeredAll == 0 ? 0 : (int) Math.round(disposedAll * 100.0 / registeredAll);
            return new DashboardStats.Registrar(
                    rs.getInt("pending"),
                    rs.getInt("filed_week"),
                    rs.getInt("hearings_today"),
                    rs.getInt("rooms_today"),
                    rs.getInt("scrutiny"),
                    rs.getInt("scrutiny_overdue"),
                    rs.getInt("disposed_30"),
                    rate);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load dashboard figures: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HearingRow> todaysHearings() {
        return hearings(HEARING_SELECT + " AND h.hearing_date = CURDATE()");
    }

    @Override
    public List<CourtroomLoad> courtroomLoad() {
        String sql = """
                SELECT r.room_no, r.name, r.active, u.name AS judge_name,
                       (SELECT COUNT(*) FROM hearings h
                         WHERE h.courtroom_id = r.id AND h.hearing_date = CURDATE()
                           AND h.status <> 'CANCELLED') AS today
                FROM courtrooms r
                LEFT JOIN judges j ON j.courtroom_id = r.id
                LEFT JOIN users u  ON u.id = j.user_id
                ORDER BY r.room_no
                """;
        List<CourtroomLoad> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CourtroomLoad(rs.getString("room_no"), rs.getString("name"),
                        rs.getString("judge_name"), rs.getInt("today"), rs.getBoolean("active")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load courtrooms: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Map<CaseType, Integer> caseCountByType() {
        String sql = "SELECT case_type, COUNT(*) AS total FROM cases WHERE status IN " + ACTIVE + " GROUP BY case_type";
        Map<CaseType, Integer> counts = new HashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(CaseType.valueOf(rs.getString("case_type")), rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count cases: " + e.getMessage(), e);
        }
        return counts;
    }

    @Override
    public List<CaseSummary> scrutinyQueue(int limit) {
        String sql = """
                SELECT c.id, c.case_number, c.title, c.case_type, c.status, c.filing_date, c.urgent,
                       u.name AS note
                FROM cases c JOIN users u ON u.id = c.filed_by
                WHERE c.status = 'FILED'
                ORDER BY c.urgent DESC, c.filing_date
                LIMIT ?
                """;
        return summaries(sql, "filing_date", limit);
    }

    // ================================================================ Judge

    @Override
    public DashboardStats.Judge judgeStats(int judgeId) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM hearings WHERE judge_id = ? AND hearing_date = CURDATE()
                     AND status <> 'CANCELLED') AS today,
                  (SELECT COUNT(*) FROM cases WHERE judge_id = ? AND status IN ('REGISTERED','PENDING')) AS pending,
                  (SELECT COUNT(*) FROM cases WHERE judge_id = ? AND status = 'RESERVED') AS reserved,
                  (SELECT COUNT(*) FROM cases WHERE judge_id = ? AND status = 'DISPOSED'
                     AND disposal_date >= CURDATE() - INTERVAL 30 DAY) AS disposed
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setInt(i, judgeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new DashboardStats.Judge(rs.getInt("today"), rs.getInt("pending"),
                        rs.getInt("reserved"), rs.getInt("disposed"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load judge figures: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HearingRow> todaysHearingsForJudge(int judgeId) {
        return hearings(HEARING_SELECT + " AND h.hearing_date = CURDATE() AND h.judge_id = ?", judgeId);
    }

    @Override
    public List<CaseSummary> reservedCases(int judgeId) {
        String sql = """
                SELECT c.id, c.case_number, c.title, c.case_type, c.status, c.urgent,
                       MAX(o.order_date) AS last_order, NULL AS note
                FROM cases c LEFT JOIN orders o ON o.case_id = c.id
                WHERE c.judge_id = ? AND c.status = 'RESERVED'
                GROUP BY c.id, c.case_number, c.title, c.case_type, c.status, c.urgent
                ORDER BY last_order
                """;
        return summaries(sql, "last_order", judgeId);
    }

    @Override
    public Map<LocalDate, Integer> judgeUpcomingLoad(int judgeId, int days) {
        // TreeMap keeps the dates sorted; every day starts at 0 so empty days still show
        Map<LocalDate, Integer> load = new TreeMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= days; i++) {
            load.put(today.plusDays(i), 0);
        }
        String sql = """
                SELECT hearing_date, COUNT(*) AS total FROM hearings
                WHERE judge_id = ? AND status <> 'CANCELLED'
                  AND hearing_date > CURDATE() AND hearing_date <= CURDATE() + INTERVAL ? DAY
                GROUP BY hearing_date
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, judgeId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    load.put(rs.getDate("hearing_date").toLocalDate(), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load upcoming hearings: " + e.getMessage(), e);
        }
        return load;
    }

    // ================================================================ Lawyer

    @Override
    public DashboardStats.Lawyer lawyerStats(int lawyerId) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM cases WHERE filed_by = ? AND status IN %1$s) AS active,
                  (SELECT COUNT(*) FROM hearings h JOIN cases c ON c.id = h.case_id
                     WHERE c.filed_by = ? AND h.status = 'SCHEDULED'
                       AND h.hearing_date BETWEEN CURDATE() AND CURDATE() + INTERVAL 7 DAY) AS week,
                  (SELECT MIN(h.hearing_date) FROM hearings h JOIN cases c ON c.id = h.case_id
                     WHERE c.filed_by = ? AND h.status = 'SCHEDULED' AND h.hearing_date >= CURDATE()) AS next_date,
                  (SELECT COUNT(*) FROM cases WHERE filed_by = ? AND status = 'DEFECTIVE') AS returned,
                  (SELECT COUNT(*) FROM cases WHERE filed_by = ? AND status = 'DISPOSED') AS disposed
                """.formatted(ACTIVE);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setInt(i, lawyerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Date next = rs.getDate("next_date");
                return new DashboardStats.Lawyer(rs.getInt("active"), rs.getInt("week"),
                        next == null ? null : next.toLocalDate(),
                        rs.getInt("returned"), rs.getInt("disposed"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load lawyer figures: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HearingRow> lawyerUpcomingHearings(int lawyerId, int days) {
        return hearings(HEARING_SELECT + """
                 AND c.filed_by = ? AND h.status = 'SCHEDULED'
                 AND h.hearing_date BETWEEN CURDATE() AND CURDATE() + INTERVAL ? DAY
                """, lawyerId, days);
    }

    @Override
    public List<CaseSummary> returnedCases(int lawyerId) {
        String sql = """
                SELECT id, case_number, title, case_type, status, filing_date, urgent,
                       defect_reason AS note
                FROM cases WHERE filed_by = ? AND status = 'DEFECTIVE'
                ORDER BY filing_date
                """;
        return summaries(sql, "filing_date", lawyerId);
    }

    // ================================================================ Public

    @Override
    public Optional<CaseSummary> findPublicStatus(String caseNumber) {
        String sql = """
                SELECT c.id, c.case_number, c.title, c.case_type, c.status, c.urgent, c.next_hearing_date,
                       CONCAT('Room ', r.room_no, ' - ', u.name) AS note
                FROM cases c
                LEFT JOIN courtrooms r ON r.id = c.courtroom_id
                LEFT JOIN users u      ON u.id = c.judge_id
                WHERE c.case_number = ?
                """;
        List<CaseSummary> found = summaries(sql, "next_hearing_date", caseNumber.trim().toUpperCase());
        return found.stream().findFirst();
    }

    // ================================================================ helpers

    /** Runs a hearing query, binds the parameters in order and sorts the rows with a Comparator. */
    private List<HearingRow> hearings(String sql, Object... params) {
        List<HearingRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time time = rs.getTime("slot_time");
                    rows.add(new HearingRow(
                            rs.getDate("hearing_date").toLocalDate(),
                            time.toLocalTime(),
                            rs.getString("case_number"),
                            rs.getString("title"),
                            rs.getString("room_no"),
                            rs.getString("judge_name"),
                            CaseStatus.valueOf(rs.getString("status")),
                            rs.getBoolean("urgent")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load hearings: " + e.getMessage(), e);
        }
        rows.sort(BY_DATE_TIME_ROOM);
        return rows;
    }

    /** Runs a case query; {@code dateColumn} says which column fills CaseSummary.date. */
    private List<CaseSummary> summaries(String sql, String dateColumn, Object... params) {
        List<CaseSummary> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate(dateColumn);
                    list.add(new CaseSummary(
                            rs.getInt("id"),
                            rs.getString("case_number"),
                            rs.getString("title"),
                            CaseType.valueOf(rs.getString("case_type")),
                            CaseStatus.valueOf(rs.getString("status")),
                            date == null ? null : date.toLocalDate(),
                            rs.getString("note"),
                            rs.getBoolean("urgent")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load cases: " + e.getMessage(), e);
        }
        return list;
    }

    private void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
