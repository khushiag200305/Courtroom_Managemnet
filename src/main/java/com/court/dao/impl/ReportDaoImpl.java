package com.court.dao.impl;

import com.court.dao.ReportDao;
import com.court.dto.AuditEntry;
import com.court.model.CaseStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportDaoImpl extends JdbcSupport implements ReportDao {

    @Override
    public Map<YearMonth, Integer> filingsByMonth(int months) {
        return byMonth("filing_date", months);
    }

    @Override
    public Map<YearMonth, Integer> disposalsByMonth(int months) {
        return byMonth("disposal_date", months);
    }

    /** Counts cases per month of the given date column; months with no cases are kept as 0. */
    private Map<YearMonth, Integer> byMonth(String column, int months) {
        Map<YearMonth, Integer> result = new TreeMap<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            result.put(current.minusMonths(i), 0);
        }
        LocalDate from = current.minusMonths(months - 1L).atDay(1);
        // column comes from a fixed list inside this class, never from user input
        String sql = "SELECT YEAR(" + column + ") AS y, MONTH(" + column + ") AS m, COUNT(*) AS total "
                + "FROM cases WHERE " + column + " >= ? GROUP BY y, m";
        for (int[] row : query(sql, rs -> new int[]{rs.getInt("y"), rs.getInt("m"), rs.getInt("total")}, from)) {
            result.put(YearMonth.of(row[0], row[1]), row[2]);
        }
        return result;
    }

    @Override
    public Map<CaseStatus, Integer> countByStatus() {
        Map<CaseStatus, Integer> counts = new EnumMap<>(CaseStatus.class);
        for (CaseStatus s : CaseStatus.values()) {
            counts.put(s, 0);
        }
        query("SELECT status, COUNT(*) AS total FROM cases GROUP BY status",
                rs -> counts.put(CaseStatus.valueOf(rs.getString("status")), rs.getInt("total")));
        return counts;
    }

    @Override
    public Map<String, Integer> pendingByJudge() {
        Map<String, Integer> result = new LinkedHashMap<>();   // keeps the ORDER BY order
        query("""
                SELECT u.name, COUNT(c.id) AS total
                FROM users u LEFT JOIN cases c ON c.judge_id = u.id AND c.status IN ('REGISTERED','PENDING','RESERVED')
                WHERE u.role = 'JUDGE'
                GROUP BY u.id, u.name
                ORDER BY total DESC, u.name
                """, rs -> result.put(rs.getString("name"), rs.getInt("total")));
        return result;
    }

    @Override
    public double averageDaysToDisposal() {
        return queryOne("SELECT AVG(DATEDIFF(disposal_date, filing_date)) FROM cases WHERE status = 'DISPOSED'",
                rs -> rs.getDouble(1)).orElse(0.0);
    }

    @Override
    public Map<LocalDate, String> holidays() {
        Map<LocalDate, String> holidays = new TreeMap<>();
        query("SELECT holiday_date, description FROM holidays",
                rs -> holidays.put(date(rs, "holiday_date"), rs.getString("description")));
        return holidays;
    }

    @Override
    public List<AuditEntry> recentActivity(int limit) {
        return query("""
                SELECT a.created_at, COALESCE(u.name, 'System') AS user_name, a.action, a.details
                FROM audit_log a LEFT JOIN users u ON u.id = a.user_id
                ORDER BY a.created_at DESC, a.id DESC LIMIT ?
                """, rs -> new AuditEntry(timestamp(rs, "created_at"), rs.getString("user_name"),
                rs.getString("action"), rs.getString("details")), limit);
    }
}
