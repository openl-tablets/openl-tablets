package org.openl.rules.webstudio.service;

import org.openl.rules.security.DefaultPrivileges;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.security.standalone.service.UserInfoUserDetailsServiceImpl;
import org.openl.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

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
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (Group group : groups) {
            resultGroups.add(
                    new SimpleGroup(group.getName(), group.getDescription(), createPrivileges(group)));
        }

        return resultGroups;
    }

    @Override
    public List<org.openl.rules.security.Group> getGroupsByPrivilege(String privilege) {
        List<Group> groups = groupDao.getAllGroups();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (Group group : groups) {
            org.openl.rules.security.Group resultGroup = new SimpleGroup(
                    group.getName(), group.getDescription(), createPrivileges(group));
            if (resultGroup.hasPrivilege(DefaultPrivileges.ADMIN.name())
                    || resultGroup.hasPrivilege(privilege)) {
                resultGroups.add(resultGroup);
            }
        }

        return resultGroups;
    }

    @Override
    public org.openl.rules.security.Group getGroupByName(String name) {
        Group group = groupDao.getGroupByName(name);
        return new SimpleGroup(group.getName(), group.getDescription(), createPrivileges(group));
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

        Set<Group> includedGroups = new HashSet<Group>();
        Set<String> privileges = new HashSet<String>();
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

        Set<Group> includedGroups = new HashSet<Group>();
        Set<String> privileges = new HashSet<String>();
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

    @Transactional
    @Override
    public void deleteGroup(String name) {
        Group group = groupDao.getGroupByName(name);
        Set<Group> includedGroups = group.getIncludedGroups();

        for (User user : group.getUsers()) {
            Set<Group> groups = user.getGroups();
            groups.remove(group);
            groups.addAll(includedGroups);
            userDao.merge(user);
        }

        for (Group parentGroup : group.getParentGroups()) {
            Set<Group> groups = parentGroup.getIncludedGroups();
            groups.remove(group);
            groups.addAll(includedGroups);
            groupDao.merge(parentGroup);
        }

        groupDao.delete(group);
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

}
