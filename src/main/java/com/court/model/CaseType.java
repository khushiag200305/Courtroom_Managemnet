package com.court.model;

/**
 * Kind of case. The prefix is used in case numbers, e.g. CS/101/2026 for a civil suit.
 */
public enum CaseType {
    CIVIL("Civil", "CS"),
    CRIMINAL("Criminal", "CR"),
    FAMILY("Family", "FM"),
    PROPERTY("Property", "PR"),
    CONSUMER("Consumer", "CC"),
    OTHER("Other", "MS");

    private final String displayName;
    private final String prefix;

    CaseType(String displayName, String prefix) {
        this.displayName = displayName;
        this.prefix = prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
