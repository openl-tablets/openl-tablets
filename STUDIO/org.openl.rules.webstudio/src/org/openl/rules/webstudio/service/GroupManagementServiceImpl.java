package org.openl.rules.webstudio.service;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.service.PrivilegesEvaluator;
import org.openl.rules.security.standalone.service.UserInfoUserDetailsServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * @author Andrei Astrouski
 */
public class GroupManagementServiceImpl extends UserInfoUserDetailsServiceImpl implements GroupManagementService {

    private GroupDao groupDao;

    @Override
    public List<org.openl.rules.security.Group> getGroups() {
        List<Group> groups = groupDao.getAllGroups();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<>();

        for (Group group : groups) {
            resultGroups.add(PrivilegesEvaluator.wrap(group));
        }

        return resultGroups;
    }

    @Override
    public org.openl.rules.security.Group getGroupByName(String name) {
        Group group = groupDao.getGroupByName(name);
        return PrivilegesEvaluator.wrap(group);
    }

    @Override
    public boolean isGroupExist(String name) {
        return groupDao.getGroupByName(name) != null;
    }

    @Override
    public void addGroup(org.openl.rules.security.Group group) {
        Group persistGroup = new Group();
        persistGroup.setName(group.getName());
        persistGroup.setDescription(group.getDescription());

        Set<Group> includedGroups = new HashSet<>();
        Set<String> privileges = new HashSet<>();
        for (Privilege privilege : group.getPrivileges()) {
            String privilegeName = privilege.getName();
            if (privilege instanceof org.openl.rules.security.Group) {
                includedGroups.add(groupDao.getGroupByName(privilegeName));
            } else {
                privileges.add(privilegeName);
            }
        }
        if (!includedGroups.isEmpty()) {
            persistGroup.setIncludedGroups(includedGroups);
        }
        if (!privileges.isEmpty()) {
            persistGroup.setPrivileges(privileges);
        }

        groupDao.save(persistGroup);
    }

    @Override
    public void updateGroup(String name, org.openl.rules.security.Group group) {
        Group persistGroup = groupDao.getGroupByName(name);
        persistGroup.setName(group.getName());
        persistGroup.setDescription(group.getDescription());

        Set<Group> includedGroups = new HashSet<>();
        Set<String> privileges = new HashSet<>();
        for (Privilege privilege : group.getPrivileges()) {
            String privilegeName = privilege.getName();
            if (privilege instanceof org.openl.rules.security.Group) {
                Group includedGroup = groupDao.getGroupByName(privilegeName);
                if (!persistGroup.equals(includedGroup)) {
                    // Persisting group should not include itself
                    includedGroups.add(includedGroup);
                } else {
                    // Save all privileges of itself persisting group
                    Set<String> includedPrivileges = includedGroup.getPrivileges();
                    if (includedPrivileges != null) {
                        privileges.addAll(includedPrivileges);
                    }
                }
            } else {
                privileges.add(privilegeName);
            }
        }

        persistGroup.setIncludedGroups(!includedGroups.isEmpty() ? includedGroups : null);
        persistGroup.setPrivileges(privileges);

        groupDao.update(persistGroup);
    }

    @Override
    public void deleteGroup(String name) {
        groupDao.deleteGroupByName(name);
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

}
