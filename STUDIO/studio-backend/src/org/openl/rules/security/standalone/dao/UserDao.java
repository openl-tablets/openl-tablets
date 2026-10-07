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

    /**
     * Return the users belonging to the group. Both directly assigned users and users having
     * a matched external group with the same name are included. The users are ordered by login name.
     *
     * @param groupName group name
     * @return users of the group, or an empty list when the group is unknown or empty
     */
    List<User> getUsersInGroup(String groupName);

    /**
     * Count the users belonging to the group. A user assigned both directly and through a matched
     * external group is counted once.
     *
     * @param groupName group name
     * @return number of distinct users of the group
     */
    long countUsersInGroup(String groupName);

    List<String> findUserNames(String searchTerm, int limit);

    Set<String> getUserNames();

    Set<Group> getGroupsForUser(String loginName);

    void updateGroupsForUser(String loginName, Set<Group> groups);
}
