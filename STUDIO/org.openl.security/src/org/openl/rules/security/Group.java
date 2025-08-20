package org.openl.rules.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

/**
 * An authority containing several other authorities
 *
 * @author NSamatov
 */
public interface Group extends GrantedAuthority {

    Collection<GrantedAuthority> getPrivileges();

    /**
     * Returns true if this group contains given privilege
     *
     * @param privilege checking privilege
     * @return true if this group contains given privilege
     */
    boolean hasPrivilege(String privilege);

}
