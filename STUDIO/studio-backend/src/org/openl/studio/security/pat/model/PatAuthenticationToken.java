package org.openl.studio.security.pat.model;

import java.util.Collection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring Security authentication token for Personal Access Token (PAT) authentication.
 * <p>
 * This class extends {@link UsernamePasswordAuthenticationToken} to represent
 * an authentication token created from a valid PAT.
 * </p>
 * <p>
 * It names the token it was made from by its public id, never by its secret, so what OpenL Studio keeps for
 * the token between requests can be found again.
 * </p>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class PatAuthenticationToken extends UsernamePasswordAuthenticationToken {

    /** The public id of the token the user signed in with. */
    private final String publicId;

    public PatAuthenticationToken(Object principal,
                                  Object credentials,
                                  Collection<? extends GrantedAuthority> authorities,
                                  String publicId) {
        super(principal, credentials, authorities);
        this.publicId = publicId;
    }
}
