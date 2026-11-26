package com.court.model;

/**
 * What a judge decides at the end of a hearing, and the case status it leads to.
 */
public enum OrderOutcome {
    ADJOURNED("Adjourned", CaseStatus.PENDING, true),
    NEXT_HEARING("Listed for next hearing", CaseStatus.PENDING, true),
    RESERVED("Judgment reserved", CaseStatus.RESERVED, false),
    DISPOSED("Disposed (final judgment)", CaseStatus.DISPOSED, false);

    private final String displayName;
    private final CaseStatus resultingStatus;
    private final boolean needsNextDate;

    OrderOutcome(String displayName, CaseStatus resultingStatus, boolean needsNextDate) {
        this.displayName = displayName;
        this.resultingStatus = resultingStatus;
        this.needsNextDate = needsNextDate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CaseStatus getResultingStatus() {
        return resultingStatus;
    }

    /** Adjourned and next-hearing orders must give the date of the next hearing. */
    public boolean needsNextDate() {
        return needsNextDate;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
