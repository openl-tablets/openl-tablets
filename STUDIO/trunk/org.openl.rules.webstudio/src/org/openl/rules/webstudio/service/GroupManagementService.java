package org.openl.rules.webstudio.service;

import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.service.UserInfoUserDetailsServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andrei Astrouski
 */
public class GroupManagementService extends UserInfoUserDetailsServiceImpl {

    private GroupDao groupDao;

    public List<org.openl.rules.security.Group> getPredefinedGroups() {
        /*PredefinedGroups[] groups = PredefinedGroups.values();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (PredefinedGroups group : groups) {
            resultGroups.add(
                    new SimpleGroup(group.getDisplayName(), group.getPrivileges()));
        }

        return resultGroups;*/
        return Collections.emptyList();
    }

    public List<org.openl.rules.security.Group> getGroups() {
        List<Group> groups = groupDao.getAll();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (Group group : groups) {
            resultGroups.add(
                    new SimpleGroup(group.getName(), createPrivileges(group)));
        }

        return resultGroups;
    }

    public org.openl.rules.security.Group getGroupByName(String name) {
        Group group = groupDao.getGroupByName(name);
        return new SimpleGroup(group.getName(), createPrivileges(group));
    }

    public void deleteGroup(String name) {
        groupDao.delete(groupDao.getGroupByName(name));
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

}
