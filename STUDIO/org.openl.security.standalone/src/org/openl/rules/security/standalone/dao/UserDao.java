package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.User;

import java.util.List;

/**
 * User dao.
 *
 * @author Andrey Naumenko
 */
public interface UserDao extends Dao<User> {
    /**
     * Return User by name or <code>null</code> if no such User.
     *
     * @param name user name
     *
     * @return User or <code>null</code>.
     */
    User getUserByName(String name);

    boolean existsByName(String name);

    void deleteUserByName(String name);

    List<User> getAllUsers();
}
