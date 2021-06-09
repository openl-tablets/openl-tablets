package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
        Collection<Privilege> grantedList = new ArrayList<>();

        Set<Group> visitedGroups = new HashSet<>();
        Set<Group> groups = user.getGroups();
        for (Group group : groups) {
            Collection<Privilege> privileges = createPrivileges(group, visitedGroups);
            grantedList.add(new SimpleGroup(group.getName(), group.getDescription(), privileges));
        }
        return grantedList;
    }

    public static SimpleGroup wrap(Group group) {
        Collection<Privilege> privileges = PrivilegesEvaluator.createPrivileges(group, new HashSet<>());
        return new SimpleGroup(group.getName(), group.getDescription(), privileges);
    }

    private static Collection<Privilege> createPrivileges(Group group, Set<Group> visitedGroups) {
        visitedGroups.add(group);
        Collection<Privilege> grantedList = new ArrayList<>();

        Set<Group> groups = group.getIncludedGroups();
        for (Group persistGroup : groups) {
            if (!visitedGroups.contains(persistGroup)) {
                Collection<Privilege> privileges = createPrivileges(persistGroup, visitedGroups);
                SimpleGroup simpleGroup = new SimpleGroup(persistGroup.getName(),
                    persistGroup.getDescription(),
                    privileges);
                grantedList.add(simpleGroup);
            }
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
