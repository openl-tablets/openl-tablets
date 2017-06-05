package org.openl.rules.security.standalone.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
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
        return grantedList;
    }

    public static Collection<Privilege> createPrivileges(Group group) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        Set<Group> groups = group.getIncludedGroups();
        for (Group persistGroup : groups) {
            grantedList.add(
                    new SimpleGroup(persistGroup.getName(), persistGroup.getDescription(), createPrivileges(persistGroup)));
        }

        Set<String> privileges = group.getPrivileges();

        if (privileges != null) {
            for (String privilege : privileges) {
                grantedList.add(Privileges.valueOf(privilege));
            }
        }
        return grantedList;
    }
}
