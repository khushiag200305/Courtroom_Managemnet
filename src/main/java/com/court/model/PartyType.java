package com.court.model;

public enum PartyType {
    PETITIONER("Petitioner"),
    RESPONDENT("Respondent");

    private final String displayName;

    PartyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
