package com.court.dao.impl;

import com.court.dao.DocumentDao;
import com.court.model.CaseDocument;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DocumentDaoImpl extends JdbcSupport implements DocumentDao {

    @Override
    public List<CaseDocument> findByCase(int caseId) {
        return query("""
                SELECT d.*, u.name AS uploader FROM documents d JOIN users u ON u.id = d.uploaded_by
                WHERE d.case_id = ? ORDER BY d.uploaded_at
                """, rs -> new CaseDocument(rs.getInt("id"), rs.getInt("case_id"), rs.getString("file_name"),
                rs.getString("stored_path"), rs.getLong("file_size"), rs.getInt("uploaded_by"),
                rs.getString("uploader"), timestamp(rs, "uploaded_at")), caseId);
    }

    @Override
    public int insert(Connection con, int caseId, String fileName, String storedPath, long size, int uploadedBy)
            throws SQLException {
        return insert(con, """
                INSERT INTO documents (case_id, file_name, stored_path, file_size, uploaded_by)
                VALUES (?, ?, ?, ?, ?)
                """, caseId, fileName, storedPath, size, uploadedBy);
    }

    @Override
    public boolean delete(int documentId) {
        return update("DELETE FROM documents WHERE id = ?", documentId) > 0;
    }
}
