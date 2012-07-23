package org.openl.rules.security;

import static org.openl.rules.security.Privileges.*;
import org.springframework.security.core.GrantedAuthority;

/**
 * Predefined roles containing some privileges or roles.
 * 
 * @author NSamatov
 */
public enum PredefinedRole implements Role {
    ROLE_VIEWER(PRIVILEGE_VIEW_PROJECTS, PRIVILEGE_READ_PROJECTS),

    ROLE_PROJECTS_EDITOR(
            ROLE_VIEWER,
            PRIVILEGE_CREATE_PROJECTS,
            PRIVILEGE_EDIT_PROJECTS,
            PRIVILEGE_ERASE_PROJECTS,
            PRIVILEGE_DELETE_PROJECTS),

    ROLE_DEPLOYMENT_EDITOR(
            PRIVILEGE_DEPLOY_PROJECTS,
            PRIVILEGE_EDIT_DEPLOYMENT,
            PRIVILEGE_CREATE_DEPLOYMENT,
            PRIVILEGE_DELETE_DEPLOYMENT,
            PRIVILEGE_ERASE_DEPLOYMENT),

    ROLE_TABLE_EDITOR(ROLE_VIEWER, PRIVILEGE_CREATE_TABLES, PRIVILEGE_EDIT_TABLES, PRIVILEGE_REMOVE_TABLES),

    ROLE_TESTER(ROLE_VIEWER, PRIVILEGE_RUN, PRIVILEGE_TRACE, PRIVILEGE_BENCHMARK),

    ROLE_TABLES_ADMIN(ROLE_TABLE_EDITOR, ROLE_TESTER),

    ROLE_ADMIN {
        @Override
        public boolean hasAuthority(String authority) {
            // admin is always right, even if it is not stated directly
            return true;
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthority() {
        return toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAuthority(String authority) {
        for (GrantedAuthority auth : authorities) {
            if (auth.getAuthority().equals(authority)) {
                return true;
            }

            if (auth instanceof Role) {
                if (((Role) auth).hasAuthority(authority)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find a predefined instance of a role that is equals to a roleAuthority or null if not found
     * @param roleAuthority checking authority
     * @return a predefined instance of a role that is equals to a roleAuthority or null if not found
     */
    public static PredefinedRole findRole(String roleAuthority) {
        for (PredefinedRole role : values()) {
            if (role.getAuthority().equals(roleAuthority)) {
                return role;
            }
        }

        return null;
    }

    /**
     * Construct new role
     * 
     * @param authorities nested authorities (privileges and roles)
     */
    private PredefinedRole(GrantedAuthority... authorities) {
        this.authorities = authorities;
    }

    private final GrantedAuthority authorities[];

}
