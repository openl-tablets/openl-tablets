package org.openl.rules.webstudio.service;

import java.util.List;

import org.openl.rules.security.Group;

public interface GroupManagementService {

    List<Group> getGroups();

    List<Group> getGroupsByPrivilege(String privilege);

    Group getGroupByName(String name);

    boolean isGroupExist(String name);

    void addGroup(Group group);

    void updateGroup(String name, Group group);

    void deleteGroup(String name);

}