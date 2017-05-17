package org.openl.rules.security.standalone.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.openl.rules.security.DefaultPrivileges;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;

public final class PrivilegesEvaluator {
    private PrivilegesEvaluator() {
    }

    public static Collection<Privilege> createPrivileges(User user) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        Set<Group> groups = user.getGroups();
        for (Group group : groups) {
            grantedList.add(
                    new SimpleGroup(group.getName(), group.getDescription(), createPrivileges(group)));
        }

        grantedList.addAll(createPrivileges(user.getPrivileges()));

        return grantedList;
    }

    public static Collection<Privilege> createPrivileges(Group group) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        Set<Group> groups = group.getIncludedGroups();
        for (Group persistGroup : groups) {
            grantedList.add(
                    new SimpleGroup(persistGroup.getName(), persistGroup.getDescription(), createPrivileges(persistGroup)));
        }

        grantedList.addAll(createPrivileges(group.getPrivileges()));

        return grantedList;
    }

    private static Collection<Privilege> createPrivileges(String privileges) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        if (privileges != null && !privileges.isEmpty()) {
            for (String privilege : privileges.split(",")) {
                grantedList.add(DefaultPrivileges.valueOf(privilege));
            }
        }

        return grantedList;
    }
}
