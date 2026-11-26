package com.court.service;

import com.court.dao.CaseDao;
import com.court.dao.OrderDao;
import com.court.dao.PartyDao;
import com.court.dao.impl.CaseDaoImpl;
import com.court.dao.impl.OrderDaoImpl;
import com.court.dao.impl.PartyDaoImpl;
import com.court.model.CaseStatus;
import com.court.model.CaseType;
import com.court.model.CourtCase;
import com.court.util.AppConfig;
import com.court.util.SessionManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Archives decided (disposed) cases to a file with <b>object serialization</b>, and reads
 * such a file back (<b>deserialization</b>) so it can be viewed later - even on another computer.
 */
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class.getName());
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    // The classes written to the file. Every field must itself be Serializable
    // (String, LocalDate, enums and ArrayList all are).

    public record ArchivedOrder(LocalDate date, String outcome, String text) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public record ArchivedCase(String caseNumber, String title, CaseType type, LocalDate filed, LocalDate disposed,
                               String judge, String lawyer, String judgment, ArrayList<String> parties,
                               ArrayList<ArchivedOrder> orders) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public record Archive(LocalDateTime createdAt, String createdBy, ArrayList<ArchivedCase> cases)
            implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    private final CaseDao caseDao = new CaseDaoImpl();
    private final PartyDao partyDao = new PartyDaoImpl();
    private final OrderDao orderDao = new OrderDaoImpl();

    /** Writes every disposed case to backups/disposed-cases-DATE-TIME.ser and returns the file. */
    public Path backupDisposedCases() throws IOException {
        ArrayList<ArchivedCase> cases = new ArrayList<>();
        for (CourtCase c : caseDao.findByStatus(CaseStatus.DISPOSED)) {
            ArrayList<String> parties = new ArrayList<>();
            partyDao.findByCase(c.getId()).forEach(p -> parties.add(p.type().getDisplayName() + ": " + p.name()));
            ArrayList<ArchivedOrder> orders = new ArrayList<>();
            orderDao.findByCase(c.getId()).forEach(o ->
                    orders.add(new ArchivedOrder(o.orderDate(), o.outcome().getDisplayName(), o.text())));
            cases.add(new ArchivedCase(c.getCaseNumber(), c.getTitle(), c.getType(), c.getFilingDate(),
                    c.getDisposalDate(), c.getJudgeName(), c.getFiledByName(), c.getJudgment(), parties, orders));
        }
        String by = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getName() : "System";
        Archive archive = new Archive(LocalDateTime.now(), by, cases);

        Path folder = Path.of(AppConfig.get("backup.folder", "backups"));
        Files.createDirectories(folder);
        Path file = folder.resolve("disposed-cases-" + LocalDateTime.now().format(STAMP) + ".ser");
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file.toFile())))) {
            out.writeObject(archive);
        }
        LOG.info("Backed up " + cases.size() + " disposed cases to " + file);
        return file;
    }

    /** Reads an archive file back into objects. */
    public Archive readBackup(Path file) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file.toFile())))) {
            Object read = in.readObject();
            if (!(read instanceof Archive archive)) {
                throw new IOException("This is not a case archive file.");
            }
            return archive;
        } catch (ClassNotFoundException e) {
            throw new IOException("This file was made by a different program version.", e);
        }
    }

    public Path backupFolder() {
        return Path.of(AppConfig.get("backup.folder", "backups")).toAbsolutePath();
    }

    /** Lists existing backup files, newest first. */
    public List<Path> existingBackups() throws IOException {
        Path folder = backupFolder();
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (var files = Files.list(folder)) {
            return files.filter(p -> p.toString().endsWith(".ser"))
                    .sorted((a, b) -> b.getFileName().compareTo(a.getFileName()))
                    .toList();
        }
    }
}
