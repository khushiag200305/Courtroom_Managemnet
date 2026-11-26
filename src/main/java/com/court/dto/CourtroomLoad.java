package com.court.dto;

/**
 * A courtroom, the judge sitting there (may be null) and how many hearings it has today.
 */
public record CourtroomLoad(String roomNo, String roomName, String judgeName, int hearingsToday, boolean active) {

    public boolean hasJudge() {
        return judgeName != null;
    }
}
