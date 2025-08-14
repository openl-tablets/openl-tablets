package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;

import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.security.InheritedAuthenticationSettings;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.util.StringUtils;

/**
 * Get all privileges for the given user.
 */
public class GetUserPrivileges implements BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> {
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final String defaultGroup;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final GrantedAuthority relevantSystemWideGrantedAuthority;

    public GetUserPrivileges(UserManagementService userManagementService,
                             GroupManagementService groupManagementService,
                             GrantedAuthority relevantSystemWideGrantedAuthority,
                             RepositoryAclServiceProvider aclServiceProvider) {
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.defaultGroup = Props.text(InheritedAuthenticationSettings.DEFAULT_GROUP);
        this.aclServiceProvider = aclServiceProvider;
        this.relevantSystemWideGrantedAuthority = relevantSystemWideGrantedAuthority;
    }

    public Collection<Privilege> apply(String user, Collection<? extends GrantedAuthority> authorities) {

        Collection<Privilege> privileges = new ArrayList<>();

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
            privileges.addAll(
                    userDetails.getAuthorities().stream().map(GetUserPrivileges::toPrivilege).collect(Collectors.toList()));
        }

        return privileges;
    }

    private void mapAuthorities(Collection<? extends GrantedAuthority> authorities, Collection<Privilege> privileges) {
        for (GrantedAuthority authority : authorities) {
            String authorityName = authority.getAuthority();
            Group group = groupManagementService.getGroupByName(authorityName);
            if (group != null) {
                // Expand priveleges from the DB
                privileges.add(group);
            } else {
                privileges.add(toPrivilege(authority));
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
        return groupManagementService.getGroupByName(defaultGroup);

    }

    private static Privilege toPrivilege(GrantedAuthority authority) {
        if (authority instanceof Privilege) {
            return ((Privilege) authority);
        } else {
            return (new SimplePrivilege(authority.getAuthority()));
        }

    }

}
