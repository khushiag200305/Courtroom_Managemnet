package com.court.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for every person who can log in.
 * <p>
 * It is abstract because a plain "User" never exists on its own - every account
 * is a {@link Registrar}, {@link Judge} or {@link Lawyer}. Each subclass decides
 * which dashboard it opens (polymorphism through {@link #getDashboardFxml()}).
 */
public abstract class User {

    private int id;
    private String name;
    private String username;
    private String passwordHash;
    private String email;
    private String phone;
    private boolean active = true;
    private LocalDateTime createdAt;

    protected User() {
    }

    protected User(String name, String username, String email, String phone) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.username = Objects.requireNonNull(username, "username must not be null").trim().toLowerCase();
        this.email = email;
        this.phone = phone;
    }

    /** Which role this account has. */
    public abstract Role getRole();

    /** FXML file of the dashboard this user sees after login. */
    public abstract String getDashboardFxml();

    /** Window title for this user's dashboard. */
    public String getDashboardTitle() {
        return getRole().getDisplayName() + " Dashboard";
    }

    // ----- getters and setters (encapsulation with basic validation) -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = Objects.requireNonNull(username, "username must not be null").trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Two users are equal when they have the same database id.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id == other.id && Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return name;
    }
}
