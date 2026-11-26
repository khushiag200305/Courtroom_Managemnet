package com.court.service;

import com.court.model.CaseType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseNumbersTest {

    @Test
    void firstCaseOfTheYearStartsAt101() {
        assertEquals("CS/101/2026", CaseNumbers.next(CaseType.CIVIL, 2026, List.of()));
    }

    @Test
    void nextNumberFollowsTheHighest() {
        List<String> used = List.of("FM/101/2026", "FM/104/2026", "FM/102/2026");
        assertEquals("FM/105/2026", CaseNumbers.next(CaseType.FAMILY, 2026, used));
    }

    @Test
    void malformedNumbersAreIgnored() {
        assertEquals("CR/101/2026", CaseNumbers.next(CaseType.CRIMINAL, 2026, List.of("CR/abc/2026", "junk")));
    }
}
