package com.court.model;

import java.time.LocalDateTime;

/**
 * A file attached to a case. The file itself lives in the documents folder;
 * the database only stores where it is.
 */
public record CaseDocument(
        int id,
        int caseId,
        String fileName,
        String storedPath,
        long fileSize,
        int uploadedBy,
        String uploadedByName,
        LocalDateTime uploadedAt) {

    /** "248 KB" or "1.4 MB". */
    public String readableSize() {
        if (fileSize < 1024 * 1024) {
            return Math.max(1, Math.round(fileSize / 1024.0)) + " KB";
        }
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
