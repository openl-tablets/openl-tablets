package org.openl.studio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

import org.springframework.security.core.GrantedAuthority;

import org.openl.rules.security.Group;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;

/**
 * Get all privileges for the given user, including group-based privileges.
 * Used in ad, saml, and oauth2 modes where groups are managed externally.
 */
public class GetUserPrivileges implements BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> {
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final String defaultGroup;

    public GetUserPrivileges(UserManagementService userManagementService,
                             GroupManagementService groupManagementService,
                             String defaultGroup) {
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.defaultGroup = defaultGroup;
    }

    @Override
    public Collection<GrantedAuthority> apply(String user, Collection<? extends GrantedAuthority> authorities) {

        var privileges = new ArrayList<GrantedAuthority>();

        // Add a default group if it presents
        var defaultGroup = getDefaultGroup();
        if (defaultGroup != null) {
            privileges.add(defaultGroup);
        }

        // Map external authorities to OpenL privileges
        mapAuthorities(authorities, privileges);

        // Add authorities from the DB if exists
        var userDetails = userManagementService.getUser(user);
        if (userDetails != null) {
            privileges.addAll(userDetails.getAuthorities());
        }

        return privileges;
    }

    private void mapAuthorities(Collection<? extends GrantedAuthority> authorities, Collection<GrantedAuthority> privileges) {
        for (GrantedAuthority authority : authorities) {
            var authorityName = authority.getAuthority();
            var group = groupManagementService.getGroupByName(authorityName);
            // Expand priveleges from the DB
            privileges.add(Objects.requireNonNullElse(group, authority));
        }
    }

    private Group getDefaultGroup() {
        if (StringUtils.isBlank(defaultGroup)) {
            return null;
        }
        var group = groupManagementService.getGroupByName(defaultGroup);
        if (group != null) {
            return group;
        }
        // Create if absent
        groupManagementService.addGroup(defaultGroup, "A default group for authenticated users");
        return groupManagementService.getGroupByName(defaultGroup);

    }

}
