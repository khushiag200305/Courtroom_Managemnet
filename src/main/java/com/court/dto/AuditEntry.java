package com.court.dto;

import java.time.LocalDateTime;

/** One row of the audit log: who did what, and when. */
public record AuditEntry(LocalDateTime time, String userName, String action, String details) {
}
