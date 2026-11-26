package com.court.service;

import com.court.dao.AuditLogDao;
import com.court.dao.CourtroomDao;
import com.court.dao.impl.AuditLogDaoImpl;
import com.court.dao.impl.CourtroomDaoImpl;
import com.court.exception.DataAccessException;
import com.court.exception.ValidationException;
import com.court.model.Courtroom;
import com.court.util.SessionManager;
import com.court.util.Validators;

import java.util.List;

public class CourtroomService {

    private final CourtroomDao courtroomDao = new CourtroomDaoImpl();
    private final AuditLogDao auditLogDao = new AuditLogDaoImpl();

    public List<Courtroom> findAll() {
        return courtroomDao.findAll();
    }

    public List<Courtroom> findActive() {
        return courtroomDao.findActive();
    }

    public Courtroom save(Courtroom room) throws ValidationException {
        room.setRoomNo(Validators.roomNo(room.getRoomNo()));
        room.setName(Validators.required(room.getName(), "Courtroom name", 100));
        try {
            if (room.getId() == 0) {
                if (courtroomDao.roomNoExists(room.getRoomNo())) {
                    throw new ValidationException("Room " + room.getRoomNo() + " already exists.");
                }
                courtroomDao.save(room);
                audit("COURTROOM_ADDED", "Room " + room.getRoomNo());
            } else {
                courtroomDao.update(room);
                audit("COURTROOM_UPDATED", "Room " + room.getRoomNo());
            }
        } catch (DataAccessException e) {
            throw new ValidationException(e.getMessage());
        }
        return room;
    }

    public void setActive(Courtroom room, boolean active) throws ValidationException {
        room.setActive(active);
        save(room);
    }

    public void delete(Courtroom room) throws ValidationException {
        try {
            courtroomDao.delete(room.getId());
            audit("COURTROOM_DELETED", "Room " + room.getRoomNo());
        } catch (DataAccessException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private void audit(String action, String details) {
        Integer by = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : null;
        auditLogDao.log(by, action, details);
    }
}
