package org.openl.rules.webstudio;

import org.springframework.beans.factory.annotation.Autowired;

import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Creates a user for single user mode.
 */
public class SingleUserModeInit {

    @Autowired
    private UserManagementService userManagementService;

    public void init() {
        userManagementService.syncUserData("DEFAULT", "De", "Fault", "default@example.com", "DEFAULT");
    }
}
