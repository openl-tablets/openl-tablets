package org.openl.rules.security;

/**
 * Constants for all Privileges.
 *
 * @author Aleh Bykhavets
 * @author NSamatov
 */
public enum Privileges implements Privilege {

    ADMIN;

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getAuthority() {
        return name();
    }

}
