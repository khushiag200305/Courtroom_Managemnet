package com.court.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A case in the court. Named CourtCase because "case" is a Java keyword.
 * <p>
 * The status can only change through {@link #moveTo(CaseStatus)}, which checks the
 * rules in {@link CaseStatus#canMoveTo(CaseStatus)} - encapsulation keeps the
 * object from ever reaching an invalid state.
 */
public class CourtCase {

    private int id;
    private String caseNumber;          // null until the Registrar registers it
    private String title;
    private CaseType type;
    private String description;
    private CaseStatus status = CaseStatus.FILED;
    private boolean urgent;

    private int filedById;
    private String filedByName;
    private Integer judgeId;
    private String judgeName;
    private Integer courtroomId;
    private String roomNo;

    private LocalDate filingDate;
    private LocalDate registrationDate;
    private LocalDate nextHearingDate;
    private LocalDate disposalDate;
    private String defectReason;
    private String judgment;

    private final List<Party> parties = new ArrayList<>();

    // ------------------------------------------------------------------ behaviour

    /** Changes the status if the rules allow it; returns false otherwise. */
    public boolean moveTo(CaseStatus next) {
        if (!status.canMoveTo(next)) {
            return false;
        }
        status = next;
        return true;
    }

    public boolean isRegistered() {
        return caseNumber != null;
    }

    /** Days since filing (for the "waiting" column in the scrutiny queue). */
    public long daysSinceFiling() {
        return filingDate == null ? 0 : ChronoUnit.DAYS.between(filingDate, LocalDate.now());
    }

    /** "CS/101/2026" once registered, otherwise "Not registered". */
    public String displayNumber() {
        return caseNumber == null ? "Not registered" : caseNumber;
    }

    public Optional<Party> firstParty(PartyType type) {
        return parties.stream().filter(p -> p.type() == type).findFirst();
    }

    // ------------------------------------------------------------------ getters and setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Objects.requireNonNull(title).trim(); }

    public CaseType getType() { return type; }
    public void setType(CaseType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CaseStatus getStatus() { return status; }

    /** Only for loading from the database - normal code uses {@link #moveTo(CaseStatus)}. */
    public void setStatusFromDatabase(CaseStatus status) { this.status = status; }

    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }

    public int getFiledById() { return filedById; }
    public void setFiledById(int filedById) { this.filedById = filedById; }

    public String getFiledByName() { return filedByName; }
    public void setFiledByName(String filedByName) { this.filedByName = filedByName; }

    public Integer getJudgeId() { return judgeId; }
    public void setJudgeId(Integer judgeId) { this.judgeId = judgeId; }

    public String getJudgeName() { return judgeName; }
    public void setJudgeName(String judgeName) { this.judgeName = judgeName; }

    public Integer getCourtroomId() { return courtroomId; }
    public void setCourtroomId(Integer courtroomId) { this.courtroomId = courtroomId; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public LocalDate getFilingDate() { return filingDate; }
    public void setFilingDate(LocalDate filingDate) { this.filingDate = filingDate; }

    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }

    public LocalDate getNextHearingDate() { return nextHearingDate; }
    public void setNextHearingDate(LocalDate nextHearingDate) { this.nextHearingDate = nextHearingDate; }

    public LocalDate getDisposalDate() { return disposalDate; }
    public void setDisposalDate(LocalDate disposalDate) { this.disposalDate = disposalDate; }

    public String getDefectReason() { return defectReason; }
    public void setDefectReason(String defectReason) { this.defectReason = defectReason; }

    public String getJudgment() { return judgment; }
    public void setJudgment(String judgment) { this.judgment = judgment; }

    public List<Party> getParties() { return parties; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourtCase other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (caseNumber == null ? "" : caseNumber + "  ") + title;
    }
}
