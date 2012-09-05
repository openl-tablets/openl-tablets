package org.openl.rules.security;

import static org.openl.rules.security.PredefinedPrivileges.*;


/**
 * Predefined groups containing some privileges or groups.
 * 
 * @author NSamatov
 */
public enum PredefinedGroups implements Group {

    GROUP_VIEWER("Viewers", PRIVILEGE_VIEW_PROJECTS, PRIVILEGE_READ_PROJECTS),

    GROUP_EDITOR("Developers",
            GROUP_VIEWER,
            PRIVILEGE_CREATE_PROJECTS,
            PRIVILEGE_EDIT_PROJECTS,
            PRIVILEGE_ERASE_PROJECTS,
            PRIVILEGE_DELETE_PROJECTS,
            PRIVILEGE_CREATE_TABLES,
            PRIVILEGE_EDIT_TABLES,
            PRIVILEGE_REMOVE_TABLES),

    GROUP_DEPLOYMENT_EDITOR("Deployers",
            PRIVILEGE_DEPLOY_PROJECTS,
            PRIVILEGE_EDIT_DEPLOYMENT,
            PRIVILEGE_CREATE_DEPLOYMENT,
            PRIVILEGE_DELETE_DEPLOYMENT,
            PRIVILEGE_ERASE_DEPLOYMENT),

    GROUP_TESTER("Testers", GROUP_VIEWER, PRIVILEGE_RUN, PRIVILEGE_TRACE, PRIVILEGE_BENCHMARK),

    GROUP_ANALYST("Analysts", GROUP_TESTER, GROUP_EDITOR),

    GROUP_ADMIN("Administrators") {
        @Override
        public boolean hasPrivilege(String authority) {
            // Admin is always right, even if it is not stated directly
            return true;
        }
    };

    private final String displayName;
    private String description;
    private final Privilege[] privileges;

    /**
     * Construct new group
     * 
     * @param authorities nested authorities (privileges and groups)
     */
    private PredefinedGroups(String displayName, Privilege... privileges) {
        this.displayName = displayName;
        this.privileges = privileges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPrivilege(String privilege) {
        for (Privilege auth : privileges) {
            if (auth.getName().equals(privilege)) {
                return true;
            }

            if (auth instanceof Group) {
                if (((Group) auth).hasPrivilege(privilege)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find a predefined instance of a group that is equals to a group name or null if not found
     * @param name checking group name
     * @return a predefined instance of a group that is equals to a group name or null if not found
     */
    public static PredefinedGroups findGroup(String name) {
        for (PredefinedGroups group : values()) {
            if (group.getName().equals(name)) {
                return group;
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Privilege[] getPrivileges() {
        return privileges;
    }

    @Override
    public String getAuthority() {
        return name();
    }

}
