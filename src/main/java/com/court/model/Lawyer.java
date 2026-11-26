package com.court.model;

/**
 * Represents clients: files cases, follows hearing dates and reads orders.
 */
public class Lawyer extends User {

    private String barId;   // Bar Council enrolment number, e.g. PB/1234/2015

    public Lawyer() {
    }

    public Lawyer(String name, String username, String email, String phone, String barId) {
        super(name, username, email, phone);
        this.barId = barId;
    }

    @Override
    public Role getRole() {
        return Role.LAWYER;
    }

    @Override
    public String getDashboardFxml() {
        return "lawyer_dashboard.fxml";
    }

    public String getBarId() {
        return barId;
    }

    public void setBarId(String barId) {
        this.barId = barId;
    }
}
