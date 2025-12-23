package org.openl.studio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import org.openl.rules.security.Group;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.security.InheritedAuthenticationSettings;
import org.openl.util.StringUtils;

/**
 * Get all privileges for the given user.
 */
@Service("privilegeMapper")
public class GetUserPrivileges implements BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> {
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final String defaultGroup;

    public GetUserPrivileges(UserManagementService userManagementService,
                             GroupManagementService groupManagementService) {
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.defaultGroup = Props.text(InheritedAuthenticationSettings.DEFAULT_GROUP);
    }

    public Collection<GrantedAuthority> apply(String user, Collection<? extends GrantedAuthority> authorities) {

        Collection<GrantedAuthority> privileges = new ArrayList<>();

        // Add a default group if it presents
        Group defaultGroup = getDefaultGroup();
        if (defaultGroup != null) {
            privileges.add(defaultGroup);
        }

        // Map external authorities to OpenL privileges
        mapAuthorities(authorities, privileges);

        // Add authorities from the DB if exists
        User userDetails = userManagementService.getUser(user);
        if (userDetails != null) {
            privileges.addAll(userDetails.getAuthorities());
        }

        return privileges;
    }

    private void mapAuthorities(Collection<? extends GrantedAuthority> authorities, Collection<GrantedAuthority> privileges) {
        for (GrantedAuthority authority : authorities) {
            String authorityName = authority.getAuthority();
            Group group = groupManagementService.getGroupByName(authorityName);
            // Expand priveleges from the DB
            privileges.add(Objects.requireNonNullElse(group, authority));
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
        return groupManagementService.getGroupByName(defaultGroup);

    }

}
