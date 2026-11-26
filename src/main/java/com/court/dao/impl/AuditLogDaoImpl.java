package com.court.dao.impl;

import com.court.dao.AuditLogDao;
import com.court.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Logger;

public class AuditLogDaoImpl implements AuditLogDao {

    private static final Logger LOG = Logger.getLogger(AuditLogDaoImpl.class.getName());

    @Override
    public void log(Integer userId, String action, String details) {
        String sql = "INSERT INTO audit_log (user_id, action, details) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            // auditing must never break the main action, so only log the problem
            LOG.warning("Audit log write failed: " + e.getMessage());
        }
    }
}
