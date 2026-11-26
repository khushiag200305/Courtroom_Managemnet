package com.court.model;

import java.util.Objects;

/**
 * A physical courtroom where hearings take place.
 */
public class Courtroom {

    private int id;
    private String roomNo;
    private String name;
    private boolean active = true;

    public Courtroom() {
    }

    public Courtroom(String roomNo, String name) {
        this.roomNo = roomNo;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Courtroom other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Shown in ComboBoxes, e.g. "Room 3 - Civil Court". */
    @Override
    public String toString() {
        return "Room " + roomNo + " - " + name;
    }
}
