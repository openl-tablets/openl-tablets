package org.openl.rules.security;

import org.springframework.security.core.GrantedAuthority;

/**
 * Constants for all Privileges.
 *
 * @author Aleh Bykhavets
 * @author NSamatov
 */
public enum Privileges implements GrantedAuthority {

    ADMIN;

    @Override
    public String getAuthority() {
        return name();
    }

}
