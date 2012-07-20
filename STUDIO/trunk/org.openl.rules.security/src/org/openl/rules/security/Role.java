package org.openl.rules.security;

import org.springframework.security.core.GrantedAuthority;

/**
 * An authority containing several other authorities
 * 
 * @author NSamatov
 */
public interface Role extends GrantedAuthority {
    /**
     * Returns true if this role contains given authority
     * 
     * @param authority checking authority
     * @return true if this role contains given authority
     */
    boolean hasAuthority(String authority);
}