package org.openl.studio.security;

import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Updates user details in the DB.
 */
@Service("syncUserData")
public class SyncUserData implements Consumer<SimpleUser> {
    private final UserManagementService userManagementService;
    private final AdminUsers adminUsersInitializer;
    private final ExternalGroupService externalGroupService;

    public SyncUserData(UserManagementService userManagementService,
                        AdminUsers adminUsersInitializer,
                        ExternalGroupService externalGroupService) {
        this.userManagementService = userManagementService;
        this.adminUsersInitializer = adminUsersInitializer;
        this.externalGroupService = externalGroupService;
    }

    @Override
    public void accept(SimpleUser user) {

        var username = user.getUsername();
        var firstName = user.getFirstName();
        var lastName = user.getLastName();
        var email = user.getEmail();
        var displayName = user.getDisplayName();

        // Update User details
        userManagementService.syncUserData(username, firstName, lastName, email, displayName);

        // Initialize admin privileges
        adminUsersInitializer.initIfSuperuser(username);

        // Store all external authorities
        externalGroupService.mergeAllForUser(username, user.getAuthorities());

    }

}
