package com.court.dao.impl;

import com.court.dao.PartyDao;
import com.court.model.Party;
import com.court.model.PartyType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PartyDaoImpl extends JdbcSupport implements PartyDao {

    @Override
    public List<Party> findByCase(int caseId) {
        return query("SELECT * FROM parties WHERE case_id = ? ORDER BY party_type, id",
                rs -> new Party(rs.getInt("id"), rs.getInt("case_id"), rs.getString("name"),
                        PartyType.valueOf(rs.getString("party_type")), rs.getString("address"), rs.getString("phone")),
                caseId);
    }

    @Override
    public void insertAll(Connection con, int caseId, List<Party> parties) throws SQLException {
        for (Party p : parties) {
            insert(con, "INSERT INTO parties (case_id, name, party_type, address, phone) VALUES (?, ?, ?, ?, ?)",
                    caseId, p.name(), p.type(), p.address(), p.phone());
        }
    }

    @Override
    public void deleteByCase(Connection con, int caseId) throws SQLException {
        update(con, "DELETE FROM parties WHERE case_id = ?", caseId);
    }
}
