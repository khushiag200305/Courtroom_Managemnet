package com.court.model;

/**
 * Runs the court office: manages users and courtrooms, checks new filings,
 * schedules hearings and publishes the cause list.
 */
public class Registrar extends User {

    public Registrar() {
    }

    public Registrar(String name, String username, String email, String phone) {
        super(name, username, email, phone);
    }

    @Override
    public Role getRole() {
        return Role.REGISTRAR;
    }

    @Override
    public String getDashboardFxml() {
        return "registrar_dashboard.fxml";
    }
}
