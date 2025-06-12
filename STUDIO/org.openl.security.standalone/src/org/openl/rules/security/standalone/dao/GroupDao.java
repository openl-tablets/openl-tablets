package org.openl.rules.security.standalone.dao;

import java.util.List;
import java.util.Set;

import org.openl.rules.security.standalone.persistence.Group;

/**
 * Group dao.
 *
 * @author Andrey Naumenko
 */
public interface GroupDao extends Dao<Group> {

    Group getGroupByName(String name);

    boolean existsByName(String name);

    Group getGroupById(Long id);

    void deleteGroupById(Long id);

    void deleteGroupByName(String name);

    List<Group> getAllGroups();

    long countUsersInGroup(String groupName);

    Set<String> getGroupNames();
}
