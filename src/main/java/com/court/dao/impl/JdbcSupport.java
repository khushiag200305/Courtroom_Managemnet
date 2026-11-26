package com.court.dao.impl;

import com.court.exception.DataAccessException;
import com.court.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC helpers for the DAO classes (abstract: it is only meant to be extended).
 * Every query uses a PreparedStatement and try-with-resources.
 */
public abstract class JdbcSupport {

    /** Turns the current ResultSet row into an object. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ------------------------------------------------------------------ reads

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load data: " + e.getMessage(), e);
        }
        return list;
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    protected int queryInt(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getInt(1), params).orElse(0);
    }

    // ------------------------------------------------------------------ writes

    /** UPDATE / DELETE on its own connection; returns the number of rows changed. */
    protected int update(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return update(con, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the change: " + e.getMessage(), e);
        }
    }

    /** UPDATE / DELETE inside a caller's transaction. */
    protected int update(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT inside a caller's transaction; returns the generated id. */
    protected int insert(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** INSERT on its own connection; returns the generated id. */
    protected int insert(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return insert(con, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ conversions

    /** Binds parameters in order; java.time and enums are converted to JDBC types. */
    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof LocalDate d) {
                ps.setDate(i + 1, Date.valueOf(d));
            } else if (value instanceof LocalTime t) {
                ps.setTime(i + 1, Time.valueOf(t));
            } else if (value instanceof Enum<?> e) {
                ps.setString(i + 1, e.name());
            } else if (value == null) {
                ps.setNull(i + 1, java.sql.Types.NULL);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    protected static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    protected static LocalTime time(ResultSet rs, String column) throws SQLException {
        Time t = rs.getTime(column);
        return t == null ? null : t.toLocalTime();
    }

    protected static LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
