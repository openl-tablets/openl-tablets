package org.openl.rules.security;

/**
 * Constants for all Privileges.
 * 
 * @author Aleh Bykhavets
 * @author NSamatov
 */
public enum Privileges implements Privilege {

    VIEW_PROJECTS("View Projects"),
    CREATE_PROJECTS("Create Projects"),
    EDIT_PROJECTS("Edit Projects"),
    ERASE_PROJECTS("Erase Projects"),
    DELETE_PROJECTS("Delete Projects"),
    UNLOCK_PROJECTS("Unlock Projects"),

    DEPLOY_PROJECTS("Deploy Projects"),

    CREATE_DEPLOYMENT("Create Deploy Configuration"),
    EDIT_DEPLOYMENT("Edit Deploy Configuration"),
    DELETE_DEPLOYMENT("Delete Deploy Configuration"),
    ERASE_DEPLOYMENT("Erase Deploy Configuration"),
    UNLOCK_DEPLOYMENT("Unlock Deploy Configuration"),

    CREATE_TABLES("Create Tables"),
    EDIT_TABLES("Edit Tables"),
    REMOVE_TABLES("Remove Tables"),

    RUN("Run Tables"),
    TRACE("Trace Tables"),
    BENCHMARK("Benchmark Tables"),

    ADMIN("Administrate");

    private final String displayName;

    private Privileges(String displayName) {
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
