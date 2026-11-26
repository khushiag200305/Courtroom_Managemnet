package com.court.service;

import com.court.dao.HearingDao;
import com.court.dao.impl.HearingDaoImpl;
import com.court.model.Hearing;
import com.court.util.AppConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the daily cause list (the list of cases each court will hear on a day) and
 * exports it as a text or CSV file using <b>character streams</b> ({@link BufferedWriter}).
 */
public class CauseListService {

    private static final DateTimeFormatter TITLE_DATE = DateTimeFormatter.ofPattern("EEEE, dd-MM-yyyy");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    /** Cause list order: courtroom first, then time. */
    public static final Comparator<Hearing> BY_ROOM_THEN_TIME =
            Comparator.comparing(Hearing::roomNo).thenComparing(Hearing::time);

    private final HearingDao hearingDao = new HearingDaoImpl();

    /** Hearings on a date, optionally for one courtroom (null = all rooms). */
    public List<Hearing> causeList(LocalDate date, Integer courtroomId) {
        return hearingDao.findByDate(date).stream()
                .filter(h -> courtroomId == null || h.courtroomId() == courtroomId)
                .sorted(BY_ROOM_THEN_TIME)
                .toList();
    }

    /** Groups the list by courtroom, keeping room order (LinkedHashMap). */
    public static Map<String, List<Hearing>> byCourtroom(List<Hearing> list) {
        return list.stream().collect(Collectors.groupingBy(
                h -> "Court room " + h.roomNo() + " - " + h.judgeName(),
                LinkedHashMap::new, Collectors.toList()));
    }

    /** Writes a printable cause list, e.g. reports/cause-list-2026-11-26.txt. */
    public Path exportText(LocalDate date, List<Hearing> list) throws IOException {
        Path file = reportsFolder().resolve("cause-list-" + date.format(FILE_DATE) + ".txt");
        String court = AppConfig.get("court.name", "District Court").toUpperCase();
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            line(out, center(court, 78));
            line(out, center("CAUSE LIST FOR " + date.format(TITLE_DATE).toUpperCase(), 78));
            line(out, "=".repeat(78));
            if (list.isEmpty()) {
                line(out, "No cases are listed for this day.");
            }
            for (Map.Entry<String, List<Hearing>> court1 : byCourtroom(list).entrySet()) {
                out.newLine();
                line(out, court1.getKey().toUpperCase());
                line(out, "-".repeat(78));
                line(out, String.format("%-4s %-9s %-14s %s", "Sr.", "Time", "Case no.", "Parties"));
                int sr = 1;
                for (Hearing h : court1.getValue()) {
                    line(out, String.format("%-4d %-9s %-14s %s", sr++, h.time().format(TIME), h.caseNumber(),
                            h.caseTitle()));
                }
            }
            out.newLine();
            line(out, "=".repeat(78));
            line(out, "Total cases listed: " + list.size() + "      Generated on " + LocalDateTime.now().format(STAMP));
        }
        return file;
    }

    /** Writes the same list as CSV, which opens in Excel or Numbers. */
    public Path exportCsv(LocalDate date, List<Hearing> list) throws IOException {
        Path file = reportsFolder().resolve("cause-list-" + date.format(FILE_DATE) + ".csv");
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            line(out, "sr_no,date,time,courtroom,judge,case_number,parties,case_status");
            int sr = 1;
            for (Hearing h : list) {
                line(out, String.join(",",
                        String.valueOf(sr++), date.toString(), h.time().toString(), h.roomNo(),
                        csv(h.judgeName()), h.caseNumber(), csv(h.caseTitle()), h.caseStatus().getDisplayName()));
            }
        }
        return file;
    }

    static Path reportsFolder() throws IOException {
        Path folder = Path.of(AppConfig.get("reports.folder", "reports"));
        Files.createDirectories(folder);
        return folder;
    }

    /** Quotes a CSV value if it contains a comma or quote. */
    static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void line(BufferedWriter out, String text) throws IOException {
        out.write(text);
        out.newLine();
    }

    private static String center(String text, int width) {
        int pad = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(pad) + text;
    }
}
