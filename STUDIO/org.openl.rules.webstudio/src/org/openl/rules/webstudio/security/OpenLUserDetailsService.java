package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.util.StringUtils;

public class OpenLUserDetailsService implements Function<SimpleUser, SimpleUser> {
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final String defaultGroup;
    private final AdminUsers adminUsersInitializer;
    private final ExternalGroupService externalGroupService;

    public OpenLUserDetailsService(UserManagementService userManagementService,
            GroupManagementService groupManagementService,
            AdminUsers adminUsersInitializer,
            ExternalGroupService externalGroupService) {
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.adminUsersInitializer = adminUsersInitializer;
        this.defaultGroup = Props.text("security.default-group");
        this.externalGroupService = externalGroupService;
    }

    public SimpleUser apply(SimpleUser user) {

        List<Privilege> privileges = new ArrayList<>();

        // Add a default group if it presents
        Group defaultGroup = getDefaultGroup();
        if (defaultGroup != null) {
            privileges.add(defaultGroup);
        }

        // Map external authorities to OpenL privileges
        mapAuthorities(user.getAuthorities(), privileges);

        SimpleUser simpleUser = SimpleUser.builder()
            .setFirstName(user.getFirstName())
            .setLastName(user.getLastName())
            .setUsername(user.getUsername())
            .setPasswordHash(user.getPassword())
            .setPrivileges(privileges)
            .setEmail(user.getEmail())
            .setDisplayName(user.getDisplayName())
            .setExternalFlags(user.getExternalFlags())
            .build();

        syncUserWithDB(simpleUser, user.getAuthorities());

        return simpleUser;
    }

    private User syncUserWithDB(SimpleUser simpleUser, Collection<Privilege> externalGroups) {
        adminUsersInitializer.initIfSuperuser(simpleUser.getUsername());
        User userDetails = userManagementService.getUser(simpleUser.getUsername());
        if (userDetails == null) {
            // Create a new user
            userManagementService.addUser(simpleUser.getUsername(),
                simpleUser.getFirstName(),
                simpleUser.getLastName(),
                null,
                simpleUser.getEmail(),
                simpleUser.getDisplayName(),
                simpleUser.getExternalFlags());
        } else {
            syncUserData(userDetails, simpleUser);
            // Update exists
            userManagementService.updateUserData(simpleUser.getUsername(),
                simpleUser.getFirstName(),
                simpleUser.getLastName(),
                simpleUser.getEmail(),
                simpleUser.getDisplayName(),
                simpleUser.getExternalFlags());
        }
        if (simpleUser.getExternalFlags().isSyncExternalGroups()) {
            externalGroupService.mergeAllForUser(simpleUser.getUsername(), externalGroups);
        }
        return userDetails;
    }

    private void syncUserData(User userDetails, SimpleUser simpleUser) {
        // Add authorities from the DB
        simpleUser.getAuthorities().addAll((Collection<? extends Privilege>) userDetails.getAuthorities());

            UserExternalFlags externalFlags = simpleUser.getExternalFlags();
            if (!externalFlags.isDisplayNameExternal()) {
                String displayName = userDetails.getDisplayName();

                // try to restore display name from previous pattern
                String firstName = StringUtils.trimToEmpty(simpleUser.getFirstName());
                String lastName = StringUtils.trimToEmpty(simpleUser.getLastName());
                String prevFirstName = StringUtils.trimToEmpty(userDetails.getFirstName());
                String prevLastName = StringUtils.trimToEmpty(userDetails.getLastName());
                String firstLastCase = StringUtils.trimToEmpty(prevFirstName + " " + prevLastName);
                String lastFirstCase = StringUtils.trimToEmpty(prevLastName + " " + prevFirstName);
                // preventing of removing existing display name pattern match by all empty fields from external service
                if (externalFlags.isFirstNameExternal() || externalFlags.isLastNameExternal()) {
                    String syncFirstName = externalFlags.isFirstNameExternal() ? firstName : prevFirstName;
                    String syncLastName = externalFlags.isLastNameExternal() ? lastName : prevLastName;
                    if (Objects.equals(userDetails.getDisplayName(), firstLastCase)) {
                        displayName = syncFirstName + " " + syncLastName;
                    } else if (Objects.equals(userDetails.getDisplayName(), lastFirstCase)) {
                        displayName = syncLastName + " " + syncFirstName;
                    }
                }
                simpleUser.setDisplayName(StringUtils.trimToEmpty(displayName));
            }
            if (!externalFlags.isFirstNameExternal()) {
                simpleUser.setFirstName(userDetails.getFirstName());
            }
            if (!externalFlags.isLastNameExternal()) {
                simpleUser.setLastName(userDetails.getLastName());
            }

            if (!externalFlags.isEmailExternal()) {
                simpleUser.setEmail(userDetails.getEmail());
                UserExternalFlags.Builder withNewEmailVerifiedFlags = UserExternalFlags.builder(externalFlags);
                withNewEmailVerifiedFlags.applyFeature(UserExternalFlags.Feature.EMAIL_VERIFIED,
                        userDetails.getExternalFlags().isEmailVerified());
                simpleUser.setExternalFlags(withNewEmailVerifiedFlags.build());
            }
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
