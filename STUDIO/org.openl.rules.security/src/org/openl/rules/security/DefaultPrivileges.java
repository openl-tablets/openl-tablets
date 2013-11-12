package org.openl.rules.security;


/**
 * Constants for all Privileges.
 * 
 * @author Aleh Bykhavets
 * @author NSamatov
 */
public enum DefaultPrivileges implements Privilege {

    PRIVILEGE_VIEW_PROJECTS ("View Projects"),
    PRIVILEGE_CREATE_PROJECTS ("Create Projects"),
    PRIVILEGE_EDIT_PROJECTS ("Edit Projects"),
    PRIVILEGE_ERASE_PROJECTS ("Erase Projects"),
    PRIVILEGE_DELETE_PROJECTS ("Delete Projects"),
    PRIVILEGE_UNLOCK_PROJECTS ("Unlock Projects"),

    PRIVILEGE_DEPLOY_PROJECTS ("Deploy Projects"),

    PRIVILEGE_CREATE_DEPLOYMENT ("Create Deploy Configuration"),
    PRIVILEGE_EDIT_DEPLOYMENT ("Edit Deploy Configuration"),
    PRIVILEGE_DELETE_DEPLOYMENT ("Delete Deploy Configuration"),
    PRIVILEGE_ERASE_DEPLOYMENT ("Erase Deploy Configuration"),
    PRIVILEGE_UNLOCK_DEPLOYMENT ("Unlock Deploy Configuration"),

    PRIVILEGE_CREATE_TABLES ("Create Tables"),
    PRIVILEGE_EDIT_TABLES ("Edit Tables"),
    PRIVILEGE_REMOVE_TABLES ("Remove Tables"),

    PRIVILEGE_RUN ("Run Tables"),
    PRIVILEGE_TRACE ("Trace Tables"),
    PRIVILEGE_BENCHMARK ("Benchmark Tables"),

    PRIVILEGE_ADMINISTRATE ("Administrate"),

    PRIVILEGE_ALL ("NO RESTRICTIONS");

    private final String displayName;

    private DefaultPrivileges(String displayName) {
        this.displayName = displayName;
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
    public String getAuthority() {
        return name();
    }

}
