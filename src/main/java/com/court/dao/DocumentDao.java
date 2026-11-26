package com.court.dao;

import com.court.model.CaseDocument;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DocumentDao {

    List<CaseDocument> findByCase(int caseId);

    int insert(Connection con, int caseId, String fileName, String storedPath, long size, int uploadedBy)
            throws SQLException;

    boolean delete(int documentId);
}
