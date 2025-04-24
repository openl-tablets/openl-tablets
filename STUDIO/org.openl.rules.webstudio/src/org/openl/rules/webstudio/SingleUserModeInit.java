package org.openl.rules.webstudio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Creates a user for single user mode.
 */
public class SingleUserModeInit {

    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String displayName;

    private final UserManagementService userManagementService;

    @Autowired
    public SingleUserModeInit(UserManagementService userManagementService,
                              @Value("${security.single.username}") String username,
                              @Value("${security.single.first-name}") String firstName,
                              @Value("${security.single.last-name}") String lastName,
                              @Value("${security.single.email}") String email,
                              @Value("${security.single.display-name}") String displayName) {
        this.userManagementService = userManagementService;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void init() {
        userManagementService.syncUserData(username, firstName, lastName, email, displayName);
    }
}
