package org.openl.rules.security;

import org.springframework.security.core.GrantedAuthority;

/**
 * Constants for all Privileges.
 * 
 * @author Aleh Bykhavets
 * @author NSamatov
 */
public enum Privileges implements GrantedAuthority {
    PRIVILEGE_VIEW_PROJECTS,
    PRIVILEGE_READ_PROJECTS,
    PRIVILEGE_CREATE_PROJECTS,
    PRIVILEGE_EDIT_PROJECTS,
    PRIVILEGE_ERASE_PROJECTS,
    PRIVILEGE_DELETE_PROJECTS,

    PRIVILEGE_DEPLOY_PROJECTS,

    PRIVILEGE_EDIT_DEPLOYMENT,
    PRIVILEGE_CREATE_DEPLOYMENT,
    PRIVILEGE_DELETE_DEPLOYMENT,
    PRIVILEGE_ERASE_DEPLOYMENT,

    PRIVILEGE_CREATE_TABLES,
    PRIVILEGE_EDIT_TABLES,
    PRIVILEGE_REMOVE_TABLES,

    PRIVILEGE_RUN,
    PRIVILEGE_TRACE,
    PRIVILEGE_BENCHMARK;

    @Override
    public String getAuthority() {
        return toString();
    }
}
