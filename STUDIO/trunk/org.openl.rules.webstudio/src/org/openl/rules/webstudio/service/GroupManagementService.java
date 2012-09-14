package org.openl.rules.webstudio.service;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.service.UserInfoUserDetailsServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * @author Andrei Astrouski
 */
public class GroupManagementService extends UserInfoUserDetailsServiceImpl {

    private GroupDao groupDao;

    public List<org.openl.rules.security.Group> getGroups() {
        List<Group> groups = groupDao.getAll();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (Group group : groups) {
            resultGroups.add(
                    new SimpleGroup(group.getName(), group.getDescription(), createPrivileges(group)));
        }

        return resultGroups;
    }

    public org.openl.rules.security.Group getGroupByName(String name) {
        Group group = groupDao.getGroupByName(name);
        return new SimpleGroup(group.getName(), group.getDescription(), createPrivileges(group));
    }

    public boolean isGroupExist(String name) {
        return groupDao.getGroupByName(name) != null;
    }

    public void addGroup(org.openl.rules.security.Group group) {
        Group persistGroup = new Group();
        persistGroup.setName(group.getName());
        persistGroup.setDescription(group.getDescription());

        Set<Group> includedGroups = new HashSet<Group>();
        List<String> privileges = new ArrayList<String>();
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
            persistGroup.setPrivileges(StringUtils.join(privileges, ","));
        }

        groupDao.save(persistGroup);
    }

    public void deleteGroup(String name) {
        groupDao.delete(groupDao.getGroupByName(name));
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

}
