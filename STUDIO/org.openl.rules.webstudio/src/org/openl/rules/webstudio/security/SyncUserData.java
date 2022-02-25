package org.openl.rules.webstudio.security;

import java.util.function.Consumer;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Updates user details in the DB.
 */
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

    public void accept(SimpleUser user) {

        String username = user.getUsername();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String email = user.getEmail();
        String displayName = user.getDisplayName();

        // Update User details
        userManagementService.syncUserData(username, firstName, lastName, email, displayName);

        // Initialize admin privileges
        adminUsersInitializer.initIfSuperuser(username);

        // Store all external authorities
        externalGroupService.mergeAllForUser(username, user.getAuthorities());

    }

}
