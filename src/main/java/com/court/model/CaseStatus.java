package com.court.model;

/**
 * Life cycle of a case:
 * <pre>
 *   FILED ──► REGISTERED ──► PENDING ──► RESERVED ──► DISPOSED
 *     │                         ▲  │                     ▲
 *     ▼                         └──┘ (next hearing)      │
 *   DEFECTIVE ──► FILED (after correction)   PENDING ────┘ (decided directly)
 * </pre>
 */
public enum CaseStatus {
    FILED("Filed"),
    DEFECTIVE("Defective"),
    REGISTERED("Registered"),
    PENDING("Pending"),
    RESERVED("Reserved"),
    DISPOSED("Disposed");

    private final String displayName;

    CaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** CSS class used for the coloured status chip, e.g. "status-pending". */
    public String getStyleClass() {
        return "status-" + name().toLowerCase();
    }

    /** A case is "active" while the court is still working on it. */
    public boolean isActive() {
        return this == REGISTERED || this == PENDING || this == RESERVED;
    }

    /** Which status changes are allowed (enforced by the services). */
    public boolean canMoveTo(CaseStatus next) {
        return switch (this) {
            case FILED -> next == REGISTERED || next == DEFECTIVE;
            case DEFECTIVE -> next == FILED;
            case REGISTERED, PENDING -> next == PENDING || next == RESERVED || next == DISPOSED;
            case RESERVED -> next == DISPOSED || next == PENDING;
            case DISPOSED -> false;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
