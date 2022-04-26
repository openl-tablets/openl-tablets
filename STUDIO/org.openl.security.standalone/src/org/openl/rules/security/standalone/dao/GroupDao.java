package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.Group;

import java.util.List;

/**
 * Group dao.
 *
 * @author Andrey Naumenko
 */
public interface GroupDao extends Dao<Group> {

    Group getGroupByName(String name);

    void deleteGroupById(Long id);

    List<Group> getAllGroups();
}
