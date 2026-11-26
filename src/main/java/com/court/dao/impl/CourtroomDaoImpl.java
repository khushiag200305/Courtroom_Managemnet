package com.court.dao.impl;

import com.court.dao.CourtroomDao;
import com.court.exception.DataAccessException;
import com.court.model.Courtroom;
import com.court.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourtroomDaoImpl implements CourtroomDao {

    @Override
    public Courtroom save(Courtroom room) {
        String sql = "INSERT INTO courtrooms (room_no, name, active) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomNo());
            ps.setString(2, room.getName());
            ps.setBoolean(3, room.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    room.setId(keys.getInt(1));
                }
            }
            return room;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("Room number " + room.getRoomNo() + " already exists.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save courtroom: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Courtroom> findById(Integer id) {
        return query("SELECT * FROM courtrooms WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<Courtroom> findAll() {
        return query("SELECT * FROM courtrooms ORDER BY room_no");
    }

    @Override
    public List<Courtroom> findActive() {
        return query("SELECT * FROM courtrooms WHERE active = TRUE ORDER BY room_no");
    }

    @Override
    public boolean roomNoExists(String roomNo) {
        String sql = "SELECT 1 FROM courtrooms WHERE room_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roomNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check room number: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Courtroom room) {
        String sql = "UPDATE courtrooms SET room_no = ?, name = ?, active = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNo());
            ps.setString(2, room.getName());
            ps.setBoolean(3, room.isActive());
            ps.setInt(4, room.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("Room number " + room.getRoomNo() + " already exists.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not update courtroom: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM courtrooms WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("This courtroom has hearings or cases. Mark it inactive instead.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete courtroom: " + e.getMessage(), e);
        }
    }

    /** Runs a SELECT with optional ? parameters (varargs) and maps every row. */
    private List<Courtroom> query(String sql, Object... params) {
        List<Courtroom> rooms = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Courtroom room = new Courtroom(rs.getString("room_no"), rs.getString("name"));
                    room.setId(rs.getInt("id"));
                    room.setActive(rs.getBoolean("active"));
                    rooms.add(room);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load courtrooms: " + e.getMessage(), e);
        }
        return rooms;
    }
}
