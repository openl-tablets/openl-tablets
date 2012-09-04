package org.openl.rules.webstudio.service;

import org.openl.rules.security.PredefinedGroups;
import org.openl.rules.security.PredefinedPrivileges;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Andrei Astrouski
 */
public class GroupManagementService {

    private GroupDao groupDao;

    protected Privilege[] createPrivileges(String privilegesStr) {
        Collection<Privilege> privileges = new ArrayList<Privilege>();
        if (privilegesStr != null) {
            StringTokenizer st = new StringTokenizer(privilegesStr, ",");
            while (st.hasMoreElements()) {
                String privilege = st.nextToken();
                if (privilege.startsWith("GROUP")) {
                    privileges.add(PredefinedGroups.valueOf(privilege));
                } else {
                    privileges.add(PredefinedPrivileges.valueOf(privilege));
                }
            }
        }

        return privileges.toArray(new Privilege[privileges.size()]);
    }

    public List<org.openl.rules.security.Group> getPredefinedGroups() {
        PredefinedGroups[] groups = PredefinedGroups.values();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (PredefinedGroups group : groups) {
            resultGroups.add(
                    new SimpleGroup(group.getDisplayName(), group.getPrivileges()));
        }

        return resultGroups;
    }

    public List<org.openl.rules.security.Group> getGroups() {
        List<Group> groups = groupDao.getAll();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<org.openl.rules.security.Group>();

        for (Group group : groups) {
            resultGroups.add(
                    new SimpleGroup(group.getName(), createPrivileges(group.getPrivileges())));
        }

        return resultGroups;
    }

    public void deleteGroup(String name) {
        //groupDao.delete(groupDao.getGroupByName(name));
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

}
