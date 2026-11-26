package com.court.dao.impl;

import com.court.dao.UserDao;
import com.court.exception.DataAccessException;
import com.court.model.Judge;
import com.court.model.Lawyer;
import com.court.model.Role;
import com.court.model.User;
import com.court.model.UserFactory;
import com.court.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link UserDao}.
 * <p>
 * A user's data is spread over two tables (users + judges/lawyers), so saving and
 * updating run inside a <b>transaction</b>: either both tables change, or neither does.
 */
public class UserDaoImpl implements UserDao {

    private static final String SELECT_BASE = """
            SELECT u.id, u.name, u.username, u.password_hash, u.role, u.email, u.phone,
                   u.active, u.created_at,
                   j.designation, j.courtroom_id, c.room_no, l.bar_id
            FROM users u
            LEFT JOIN judges j     ON j.user_id = u.id
            LEFT JOIN courtrooms c ON c.id = j.courtroom_id
            LEFT JOIN lawyers l    ON l.user_id = u.id
            """;

    // ------------------------------------------------------------------ INSERT

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (name, username, password_hash, role, email, phone, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);                       // start transaction

            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, user.getRole().name());
                ps.setString(5, user.getEmail());
                ps.setString(6, user.getPhone());
                ps.setBoolean(7, user.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
            }
            saveRoleDetails(con, user, false);

            con.commit();                                   // both inserts succeeded
            return user;
        } catch (SQLIntegrityConstraintViolationException e) {
            DBConnection.rollbackQuietly(con);
            throw new DataAccessException("Username, Bar ID or courtroom is already in use.", e);
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);              // undo the half-finished insert
            throw new DataAccessException("Could not save user: " + e.getMessage(), e);
        } finally {
            closeQuietly(con);
        }
    }

    /** Inserts or updates the judges/lawyers row. Uses pattern matching for instanceof. */
    private void saveRoleDetails(Connection con, User user, boolean isUpdate) throws SQLException {
        if (user instanceof Judge judge) {
            String sql = isUpdate
                    ? "UPDATE judges SET designation = ?, courtroom_id = ? WHERE user_id = ?"
                    : "INSERT INTO judges (designation, courtroom_id, user_id) VALUES (?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, judge.getDesignation());
                if (judge.getCourtroomId() == null) {
                    ps.setNull(2, Types.INTEGER);
                } else {
                    ps.setInt(2, judge.getCourtroomId());
                }
                ps.setInt(3, judge.getId());
                ps.executeUpdate();
            }
        } else if (user instanceof Lawyer lawyer) {
            String sql = isUpdate
                    ? "UPDATE lawyers SET bar_id = ? WHERE user_id = ?"
                    : "INSERT INTO lawyers (bar_id, user_id) VALUES (?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, lawyer.getBarId());
                ps.setInt(2, lawyer.getId());
                ps.executeUpdate();
            }
        }
        // Registrar has no extra table
    }

    // ------------------------------------------------------------------ SELECT

    @Override
    public Optional<User> findById(Integer id) {
        return findOne(SELECT_BASE + " WHERE u.id = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOne(SELECT_BASE + " WHERE u.username = ?", ps -> ps.setString(1, username.trim().toLowerCase()));
    }

    @Override
    public List<User> findAll() {
        return findMany(SELECT_BASE + " ORDER BY u.role, u.name", ps -> { });
    }

    @Override
    public List<User> findByRole(Role role) {
        return findMany(SELECT_BASE + " WHERE u.role = ? ORDER BY u.name", ps -> ps.setString(1, role.name()));
    }

    @Override
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check username: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ UPDATE

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, phone = ?, active = ? WHERE id = ?";
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            int rows;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPhone());
                ps.setBoolean(4, user.isActive());
                ps.setInt(5, user.getId());
                rows = ps.executeUpdate();
            }
            saveRoleDetails(con, user, true);
            con.commit();
            return rows > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            DBConnection.rollbackQuietly(con);
            throw new DataAccessException("Bar ID or courtroom is already assigned to someone else.", e);
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);
            throw new DataAccessException("Could not update user: " + e.getMessage(), e);
        } finally {
            closeQuietly(con);
        }
    }

    @Override
    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not change password: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ DELETE

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("This user is linked to cases or hearings. Deactivate the account instead.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete user: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Small functional interface so each query can set its own parameters with a lambda. */
    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private Optional<User> findOne(String sql, ParamSetter params) {
        List<User> list = findMany(sql, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private List<User> findMany(String sql, ParamSetter params) {
        List<User> users = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            params.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users: " + e.getMessage(), e);
        }
        return users;
    }

    /** Turns one ResultSet row into the correct User subclass. */
    private User mapRow(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        User user = UserFactory.create(role);
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            user.setCreatedAt(created.toLocalDateTime());
        }

        if (user instanceof Judge judge) {
            judge.setDesignation(rs.getString("designation"));
            int roomId = rs.getInt("courtroom_id");
            judge.setCourtroomId(rs.wasNull() ? null : roomId);
            judge.setCourtroomName(rs.getString("room_no"));
        } else if (user instanceof Lawyer lawyer) {
            lawyer.setBarId(rs.getString("bar_id"));
        }
        return user;
    }

    private void closeQuietly(Connection con) {
        if (con == null) return;
        try {
            con.setAutoCommit(true);
            con.close();
        } catch (SQLException ignored) {
            // nothing useful to do if close fails
        }
    }
}
