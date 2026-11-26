package com.court.dao;

import java.util.List;
import java.util.Optional;

/**
 * Basic CRUD operations shared by every DAO.
 * <p>
 * Generics make it type-safe: a {@code GenericDao<User, Integer>} can only save
 * Users and find them by an Integer id - mistakes are caught at compile time.
 *
 * @param <T>  the entity type (User, Courtroom, Case, ...)
 * @param <ID> the type of its primary key
 */
public interface GenericDao<T, ID> {

    /** INSERT - returns the entity with its generated id filled in. */
    T save(T entity);

    /** SELECT by primary key. Empty Optional if nothing matches. */
    Optional<T> findById(ID id);

    /** SELECT all rows. */
    List<T> findAll();

    /** UPDATE - returns true if a row was changed. */
    boolean update(T entity);

    /** DELETE - returns true if a row was removed. */
    boolean delete(ID id);
}
