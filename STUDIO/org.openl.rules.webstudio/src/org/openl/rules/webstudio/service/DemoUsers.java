package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;

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
    private static boolean initialized = false;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private GroupManagementService groupManagementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void init() {
        if (initialized) {
            // Demo users are created already.
            return;
        }

        userManagementService.addUser(getUser("a1", "Administrators"));
        userManagementService.addUser(getUser("u0", "Testers"));
        userManagementService.addUser(getUser("u1", "Developers", "Analysts"));
        userManagementService.addUser(getUser("u2", "Viewers"));
        userManagementService.addUser(getUser("u3", "Viewers"));
        userManagementService.addUser(getUser("u4", "Deployers"));
        userManagementService.addUser(getUser("user", "Viewers"));

        initialized = true;
    }

    private SimpleUser getUser(String user, String... groups) {
        String password = passwordEncoder.encode(user);
        List<Privilege> privileges = new ArrayList<Privilege>(groups.length);
        for (String group : groups) {
            privileges.add(groupManagementService.getGroupByName(group));
        }
        return new SimpleUser(null, null, user, password, privileges);
    }
}
