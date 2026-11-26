package com.court.dao;

import com.court.model.CourtOrder;
import com.court.model.OrderOutcome;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface OrderDao {

    List<CourtOrder> findByCase(int caseId);

    List<CourtOrder> findByLawyer(int lawyerId);

    List<CourtOrder> findByJudge(int judgeId, int limit);

    int insert(Connection con, int caseId, Integer hearingId, int judgeId, String text,
               OrderOutcome outcome, LocalDate nextDate, LocalDate orderDate) throws SQLException;
}
