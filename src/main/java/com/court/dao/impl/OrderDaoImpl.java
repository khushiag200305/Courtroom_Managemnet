package com.court.dao.impl;

import com.court.dao.OrderDao;
import com.court.model.CourtOrder;
import com.court.model.OrderOutcome;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class OrderDaoImpl extends JdbcSupport implements OrderDao {

    private static final String SELECT = """
            SELECT o.*, c.case_number, c.title, u.name AS judge_name
            FROM orders o
            JOIN cases c ON c.id = o.case_id
            JOIN users u ON u.id = o.judge_id
            """;

    @Override
    public List<CourtOrder> findByCase(int caseId) {
        return query(SELECT + " WHERE o.case_id = ? ORDER BY o.order_date DESC, o.id DESC", this::map, caseId);
    }

    @Override
    public List<CourtOrder> findByLawyer(int lawyerId) {
        return query(SELECT + " WHERE c.filed_by = ? ORDER BY o.order_date DESC, o.id DESC", this::map, lawyerId);
    }

    @Override
    public List<CourtOrder> findByJudge(int judgeId, int limit) {
        return query(SELECT + " WHERE o.judge_id = ? ORDER BY o.order_date DESC, o.id DESC LIMIT ?",
                this::map, judgeId, limit);
    }

    @Override
    public int insert(Connection con, int caseId, Integer hearingId, int judgeId, String text,
                      OrderOutcome outcome, LocalDate nextDate, LocalDate orderDate) throws SQLException {
        return insert(con, """
                INSERT INTO orders (case_id, hearing_id, judge_id, order_text, outcome, next_date, order_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, caseId, hearingId, judgeId, text, outcome, nextDate, orderDate);
    }

    private CourtOrder map(ResultSet rs) throws SQLException {
        return new CourtOrder(
                rs.getInt("id"),
                rs.getInt("case_id"),
                rs.getString("case_number"),
                rs.getString("title"),
                nullableInt(rs, "hearing_id"),
                rs.getInt("judge_id"),
                rs.getString("judge_name"),
                rs.getString("order_text"),
                OrderOutcome.valueOf(rs.getString("outcome")),
                date(rs, "next_date"),
                date(rs, "order_date"));
    }
}
