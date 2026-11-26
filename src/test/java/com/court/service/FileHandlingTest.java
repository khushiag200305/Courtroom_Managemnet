package com.court.service;

import com.court.exception.ValidationException;
import com.court.model.CaseType;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Byte streams, character-stream CSV helpers and object serialization - no database needed. */
class FileHandlingTest {

    @Test
    void byteCopyKeepsEveryByte() throws IOException {
        Path dir = Files.createTempDirectory("court-test");
        byte[] data = new byte[20_000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 251);
        Path source = Files.write(dir.resolve("petition.pdf"), data);
        Path target = dir.resolve("copy.pdf");

        long copied = DocumentService.copyBytes(source, target);

        assertEquals(data.length, copied);
        assertArrayEquals(data, Files.readAllBytes(target));
    }

    @Test
    void unsupportedAndEmptyFilesAreRejected() throws IOException {
        Path dir = Files.createTempDirectory("court-test");
        Path exe = Files.writeString(dir.resolve("virus.exe"), "x");
        Path empty = Files.createFile(dir.resolve("empty.pdf"));
        DocumentService service = new DocumentService();
        assertThrows(ValidationException.class, () -> service.check(exe));
        assertThrows(ValidationException.class, () -> service.check(empty));
        assertEquals("pdf", DocumentService.extension("Petition.PDF"));
    }

    @Test
    void csvValuesWithCommasAreQuoted() {
        assertEquals("\"Bansal Traders, Patiala\"", CauseListService.csv("Bansal Traders, Patiala"));
        assertEquals("\"Say \"\"hi\"\"\"", CauseListService.csv("Say \"hi\""));
        assertEquals("Plain", CauseListService.csv("Plain"));
    }

    @Test
    void archiveSurvivesSerializationRoundTrip() throws IOException {
        ArrayList<BackupService.ArchivedOrder> orders = new ArrayList<>(List.of(
                new BackupService.ArchivedOrder(LocalDate.of(2026, 10, 1), "Disposed", "Suit decreed.")));
        ArrayList<String> parties = new ArrayList<>(List.of("Petitioner: Rana", "Respondent: Chauhan"));
        BackupService.ArchivedCase c = new BackupService.ArchivedCase("CS/120/2026", "Rana vs. Chauhan",
                CaseType.CIVIL, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 10, 1), "Justice Anil Mehta",
                "Adv. Neha Verma", "Suit decreed.", parties, orders);
        BackupService.Archive archive = new BackupService.Archive(LocalDateTime.of(2026, 11, 26, 10, 0), "Test",
                new ArrayList<>(List.of(c)));

        Path file = Files.createTempFile("archive", ".ser");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            out.writeObject(archive);
        }
        BackupService.Archive back = new BackupService().readBackup(file);

        assertEquals(archive, back);                       // records compare field by field
        assertEquals("CS/120/2026", back.cases().get(0).caseNumber());
    }
}
