package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.persistence.Group;

public final class PrivilegesEvaluator {
    private PrivilegesEvaluator() {
    }

    public static Collection<GrantedAuthority> createPrivileges(Set<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        var grantedList = new ArrayList<GrantedAuthority>();
        for (Group group : groups) {
            var privileges = createPrivileges(group);
            grantedList.add(new SimpleGroup(group.getName(), group.getDescription(), privileges));
        }
        return grantedList;
    }

    public static SimpleGroup wrap(Group group) {
        var privileges = PrivilegesEvaluator.createPrivileges(group);
        return new SimpleGroup(group.getName(), group.getDescription(), privileges);
    }

    private static Collection<GrantedAuthority> createPrivileges(Group group) {
        var grantedList = new ArrayList<GrantedAuthority>();

        Set<String> privileges = group.getPrivileges();

        if (privileges != null) {
            for (String privilege : privileges) {
                grantedList.add(Privileges.valueOf(privilege));
            }
        }
        return grantedList;
    }
}
