package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.util.StringUtils;

public class OpenLUserDetailsService implements Function<SimpleUser, SimpleUser> {
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final String defaultGroup;
    private final boolean groupsAreManagedInStudio;

    public OpenLUserDetailsService(UserManagementService userManagementService,
            GroupManagementService groupManagementService,
            boolean groupsAreManagedInStudio) {
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.groupsAreManagedInStudio = groupsAreManagedInStudio;
        this.defaultGroup = Props.text("security.default-group");
    }

    public SimpleUser apply(SimpleUser user) {

        List<Privilege> privileges = new ArrayList<>();

        // Add a default group if it presents
        Group defaultGroup = getDefaultGroup();
        if (defaultGroup != null) {
            privileges.add(defaultGroup);
        }

        String username = user.getUsername();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        User userFromDB = getUserFromDB(username, firstName, lastName);
        if (!groupsAreManagedInStudio) {
            // Map external authorities to OpenL privileges
            mapAuthorities(user.getAuthorities(), privileges);
        } else if (userFromDB != null) {
            // Add authorities from the DB
            privileges.addAll((Collection<? extends Privilege>) userFromDB.getAuthorities());
        }

        return new SimpleUser(firstName, lastName, username, user.getPassword(), privileges);
    }

    private User getUserFromDB(String username, String firstName, String lastName) {
        User userDetails = userManagementService.loadUserByUsername(username);
        if (userDetails == null) {
            // Create a new user
            userManagementService.addUser(username, firstName, lastName, null);
        } else {
            // Update exists
            userManagementService.updateUserData(username, firstName, lastName, null, true);
        }
        return userDetails;
    }

    private void mapAuthorities(Collection<? extends Privilege> authorities, List<Privilege> privileges) {
        for (Privilege authority : authorities) {
            String authorityName = authority.getAuthority();
            Group group = groupManagementService.getGroupByName(authorityName);
            if (group != null) {
                // Expand priveleges from the DB
                privileges.add(group);
            } else {
                privileges.add(authority);
            }
        }
    }

    private Group getDefaultGroup() {
        if (StringUtils.isBlank(defaultGroup)) {
            return null;
        }
        Group group = groupManagementService.getGroupByName(defaultGroup);
        if (group != null) {
            return group;
        }
        // Create if absent
        groupManagementService.addGroup(defaultGroup, "A default group for authenticated users");
        groupManagementService.updateGroup(defaultGroup,
            Collections.emptySet(),
            Collections.singleton(Privileges.VIEW_PROJECTS.getAuthority()));
        return groupManagementService.getGroupByName(defaultGroup);

    }
}
