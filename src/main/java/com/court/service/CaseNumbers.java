package com.court.service;

import com.court.model.CaseType;

import java.util.List;
import java.util.Objects;

/**
 * Generates case numbers in the format PREFIX/SERIAL/YEAR, e.g. CS/101/2026.
 * Serial numbers start at 101 for every type and year.
 */
public final class CaseNumbers {

    public static final int FIRST_SERIAL = 101;

    private CaseNumbers() {
    }

    /** The next free number, given the numbers already used for that type and year. */
    public static String next(CaseType type, int year, List<String> existing) {
        int max = existing.stream()
                .filter(Objects::nonNull)
                .map(n -> n.split("/"))
                .filter(parts -> parts.length == 3)
                .mapToInt(parts -> parseOrZero(parts[1]))
                .max()
                .orElse(0);
        // never go below the first serial, even if only malformed numbers exist
        int next = Math.max(max + 1, FIRST_SERIAL);
        return type.getPrefix() + "/" + next + "/" + year;
    }

    private static int parseOrZero(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
