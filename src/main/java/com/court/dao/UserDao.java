package com.court.dao;

import com.court.model.Role;
import com.court.model.User;

import java.util.List;
import java.util.Optional;

/**
 * User-specific queries on top of the generic CRUD operations.
 */
public interface UserDao extends GenericDao<User, Integer> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(Role role);

    boolean usernameExists(String username);

    boolean updatePassword(int userId, String newPasswordHash);
}
