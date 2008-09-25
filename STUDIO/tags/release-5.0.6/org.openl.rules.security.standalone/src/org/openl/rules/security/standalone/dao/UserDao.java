package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.User;

/**
 * User dao.
 *
 * @author Andrey Naumenko
 */
public interface UserDao extends Dao {
    /**
     * Return User by name or <code>null</code> if no such User.
     *
     * @param name user name
     *
     * @return User or <code>null</code>.
     */
    User getUserByName(String name);
}
