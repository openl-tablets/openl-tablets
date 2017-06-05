package org.openl.rules.security.standalone.dao;

import java.util.List;

/**
 * Base interface to be implemented by DAO interfaces. All Dao interfaces (like
 * UserDao, etc) should extend this interface.
 *
 * @author Andrey Naumenko
 */
public interface Dao<T> {
    /**
     * Delete given object.
     *
     * @param obj object to delete.
     */
    void delete(T obj);

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

    /**
     * Merges object.
     *
     * @param obj object to merge.
     */
    void merge(T obj);

}
