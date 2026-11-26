package com.court.service;

import com.court.dao.DocumentDao;
import com.court.dao.impl.DocumentDaoImpl;
import com.court.exception.ValidationException;
import com.court.model.CaseDocument;
import com.court.util.AppConfig;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Stores case documents (petitions, evidence, ...) in the documents folder using
 * <b>byte streams</b>: files such as PDFs and images are binary, so they are copied
 * byte by byte, not as text.
 */
public class DocumentService {

    private static final Logger LOG = Logger.getLogger(DocumentService.class.getName());
    public static final long MAX_SIZE = 10L * 1024 * 1024;   // 10 MB
    public static final int MAX_FILES = 10;
    public static final Set<String> ALLOWED = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx", "txt");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DocumentDao documentDao = new DocumentDaoImpl();

    public List<CaseDocument> findByCase(int caseId) {
        return documentDao.findByCase(caseId);
    }

    /** Checks type, size and that the file can be read - before anything is saved. */
    public void check(Path file) throws ValidationException {
        String name = file.getFileName().toString();
        String ext = extension(name);
        if (!ALLOWED.contains(ext)) {
            throw new ValidationException(name + ": only PDF, image, Word or text files can be attached.");
        }
        try {
            long size = Files.size(file);
            if (size == 0) {
                throw new ValidationException(name + " is empty.");
            }
            if (size > MAX_SIZE) {
                throw new ValidationException(name + " is larger than 10 MB.");
            }
        } catch (IOException e) {
            throw new ValidationException(name + " could not be read.");
        }
    }

    /**
     * Copies the file into documents/case-ID/ and records it in the database,
     * inside the caller's transaction.
     */
    public void store(Connection con, int caseId, Path source, int uploadedBy) throws SQLException {
        Path folder = Path.of(AppConfig.get("documents.folder", "documents"), "case-" + caseId);
        String safeName = source.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        Path target = folder.resolve(LocalDateTime.now().format(STAMP) + "-" + safeName);
        try {
            Files.createDirectories(folder);
            long size = copyBytes(source, target);
            documentDao.insert(con, caseId, source.getFileName().toString(), target.toString(), size, uploadedBy);
            LOG.info("Stored document " + target + " (" + size + " bytes)");
        } catch (IOException e) {
            // Turn it into an SQLException so the surrounding transaction is rolled back
            throw new SQLException("Could not copy " + source.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /** Byte-stream copy with an 8 KB buffer; returns the number of bytes copied. */
    static long copyBytes(Path source, Path target) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        try (InputStream in = new FileInputStream(source.toFile());
             OutputStream out = new FileOutputStream(target.toFile())) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }

    /** Where the file is on disk, or null if it is missing (e.g. sample data without files). */
    public Path locate(CaseDocument document) {
        Path path = Path.of(document.storedPath());
        return Files.exists(path) ? path.toAbsolutePath() : null;
    }

    public boolean delete(CaseDocument document) {
        try {
            Files.deleteIfExists(Path.of(document.storedPath()));
        } catch (IOException e) {
            LOG.warning("Could not delete file " + document.storedPath() + ": " + e.getMessage());
        }
        return documentDao.delete(document.id());
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
