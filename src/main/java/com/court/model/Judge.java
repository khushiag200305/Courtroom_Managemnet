package com.court.model;

/**
 * Hears the cases listed in their courtroom and records orders and judgments.
 */
public class Judge extends User {

    private String designation;
    private Integer courtroomId;     // Integer (not int) because a judge may not have a room yet
    private String courtroomName;

    public Judge() {
    }

    public Judge(String name, String username, String email, String phone, String designation) {
        super(name, username, email, phone);
        this.designation = designation;
    }

    @Override
    public Role getRole() {
        return Role.JUDGE;
    }

    @Override
    public String getDashboardFxml() {
        return "judge_dashboard.fxml";
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Integer getCourtroomId() {
        return courtroomId;
    }

    public void setCourtroomId(Integer courtroomId) {
        this.courtroomId = courtroomId;
    }

    public String getCourtroomName() {
        return courtroomName;
    }

    public void setCourtroomName(String courtroomName) {
        this.courtroomName = courtroomName;
    }

    public boolean hasCourtroom() {
        return courtroomId != null;
    }
}
