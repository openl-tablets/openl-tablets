package org.openl.rules.webstudio.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Allows to create or assign administrators from the properties file.
 * 
 * There are two cases are supported:<br>
 * 1) When roles are managed externally (in LDAP/AD for example). Then ADMIN privilege is set to groups.<br>
 * 2) When roles are managed in WebStudio. Then a group with ADMIN privilege is set to users.<br>
 *
 * @author Yury Molchan
 */
public class AdminUsers {

    @Autowired
    private UserManagementService userService;

    @Autowired
    private GroupManagementService groupService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    private Set<String> administrators;

    private static final String ADMIN = Privileges.ADMIN.name();
    private static final String ADMIN_GROUP = "Administrators";

    public void init() {
        String[] administrators = StringUtils.split(environment.getProperty("security.administrators"), ',');
        this.administrators = new HashSet<>(Arrays.asList(administrators));
    }

    public boolean isSuperuser(String username) {
        return administrators.contains(username)   ;
    }

    public void initIfSuperuser(String username) {
        if (!isSuperuser(username)) {
            return;
        }
        SimpleUser user = (SimpleUser) userService.loadUserByUsername(username);
        String adminGroup = assignPrivileges(username);
        if (user == null) {
            userService.addUser(username, null, null, passwordEncoder.encode(username));
            userService.updateAuthorities(username, Collections.singleton(adminGroup));
        } else if (!user.hasPrivilege(ADMIN)) {
            Set<String> groups = new HashSet<>();
            groups.add(adminGroup);
            user.getAuthorities().stream().filter(g -> g instanceof Group).map(Privilege::getName).forEach(groups::add);
            userService.updateAuthorities(username, groups);
        }
    }

    private String assignPrivileges(String user) {
        Group administrators = groupService.getGroupByName(ADMIN_GROUP);
        if (administrators != null) {
            if (administrators.hasPrivilege(ADMIN)) {
                return ADMIN_GROUP;
            }
        }
        for (Group group : groupService.getGroups()) {
            if (group.hasPrivilege(ADMIN)) {
                return group.getName();
            }
        }
        if (!groupService.isGroupExist(ADMIN_GROUP)) {
            groupService.addGroup(ADMIN_GROUP, "A group with ADMIN privileges (restored)");
            groupService.updateGroup(ADMIN_GROUP, Collections.emptySet(), Collections.singleton(ADMIN));
            return ADMIN_GROUP;
        }
        String group = (user + "_Group");
        if (!groupService.isGroupExist(group)) {
            groupService.addGroup(group, "A group for restoring ADMIN privileges");
        }
        groupService.updateGroup(group, Collections.emptySet(), Collections.singleton(ADMIN));
        return group;
    }

}
