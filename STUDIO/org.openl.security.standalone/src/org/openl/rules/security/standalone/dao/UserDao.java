package org.openl.rules.security.standalone.dao;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;

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
     * @return User or <code>null</code>.
     */
    User getUserByName(String name);

    boolean existsByName(String name);

    void deleteUserByName(String name);

    /**
     * Store the time of the last successful sign-in of the user. Unknown users are ignored.
     *
     * @param loginName     login name of the user
     * @param lastLoginTime time of the sign-in
     */
    void updateLastLoginTime(String loginName, Instant lastLoginTime);

    List<User> getAllUsers();

    Set<String> getUserNames();

    Set<Group> getGroupsForUser(String loginName);

    void updateGroupsForUser(String loginName, Set<Group> groups);
}
