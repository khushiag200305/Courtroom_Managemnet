package com.court.model;

import java.util.Objects;

/**
 * A petitioner or respondent in a case.
 * The compact constructor validates the data whenever a Party is created.
 */
public record Party(int id, int caseId, String name, PartyType type, String address, String phone) {

    public Party {
        Objects.requireNonNull(type, "party type is required");
        name = Objects.requireNonNull(name, "party name is required").trim();
    }

    /** Convenience constructor for a party that is not saved yet. */
    public Party(String name, PartyType type, String address, String phone) {
        this(0, 0, name, type, address, phone);
    }
}
