package org.openl.rules.webstudio.service;

import java.util.Arrays;
import java.util.HashSet;

import org.openl.rules.security.UserExternalFlags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates users for demo mode.
 *
 * @author Yury Molchan
 */
public class DemoUsers {
    /**
     * It's assumed that demo users are stored in in-memory database and are destroyed on JVM shutdown. This global
     * static variable is needed to create demo users only once. After administration settings change context is
     * refreshed and this bean is invoked again. This time it must keep previous changes, no need to create demo users
     * again.
     */
    private static boolean initialized;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void init() {
        if (initialized) {
            // Demo users are created already.
            return;
        }

        initUser("admin", "admin@example.com", "Admin", "Administrators");
        initUser("a1", "a1@example.com", "A1", "Administrators");
        initUser("u0", "u0@example.com", "U0", "Testers");
        initUser("u1", "u1@example.com", "U1", "Developers", "Analysts");
        initUser("u2", "u2@example.com", "U2", "Viewers");
        initUser("u3", "u3@example.com", "U3", "Viewers");
        initUser("u4", "u4@example.com", "U4", "Deployers");
        initUser("user", "user@example.com", "User", "Viewers");

        initialized = true;
    }

    private void initUser(String user, String email, String displayName, String... groups) {
        String password = passwordEncoder.encode(user);
        userManagementService.addUser(user, null, null, password, email, displayName, new UserExternalFlags());
        userManagementService.updateAuthorities(user, new HashSet<>(Arrays.asList(groups)));

    }
}
