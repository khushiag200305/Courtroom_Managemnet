package com.court.dao;

/**
 * Writes important actions (login, case registration, orders, ...) to the audit_log table.
 */
public interface AuditLogDao {

    void log(Integer userId, String action, String details);
}
