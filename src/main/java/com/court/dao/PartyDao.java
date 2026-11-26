package com.court.dao;

import com.court.model.Party;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PartyDao {

    List<Party> findByCase(int caseId);

    void insertAll(Connection con, int caseId, List<Party> parties) throws SQLException;

    void deleteByCase(Connection con, int caseId) throws SQLException;
}
