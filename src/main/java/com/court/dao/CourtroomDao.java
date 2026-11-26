package com.court.dao;

import com.court.model.Courtroom;

import java.util.List;

public interface CourtroomDao extends GenericDao<Courtroom, Integer> {

    List<Courtroom> findActive();

    boolean roomNoExists(String roomNo);
}
