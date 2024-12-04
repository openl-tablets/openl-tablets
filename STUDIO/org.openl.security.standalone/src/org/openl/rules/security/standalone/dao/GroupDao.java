package org.openl.rules.security.standalone.dao;

import java.util.List;

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

    List<Group> getAllGroups();
}
