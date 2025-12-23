package org.openl.studio.security.pat.model;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring Security authentication token for Personal Access Token (PAT) authentication.
 * <p>
 * This class extends {@link UsernamePasswordAuthenticationToken} to represent
 * an authentication token created from a valid PAT.
 * </p>
 */
public class PatAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public PatAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
