package org.openl.rules.security.standalone.dao;

/**
 * Base interface to be implemented by DAO interfaces. All Dao interfaces (like
 * UserDao, etc) should extend this interface.
 *
 * @author Andrey Naumenko
 */
public interface Dao<T> {

    /**
     * Saves object.
     *
     * @param obj object to save.
     */
    void save(T obj);

    /**
     * Updates object.
     *
     * @param obj object to update.
     */
    void update(T obj);
}
