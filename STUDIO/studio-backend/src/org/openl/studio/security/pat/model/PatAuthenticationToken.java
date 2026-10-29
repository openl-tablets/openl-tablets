package org.openl.studio.security.pat.model;

import java.util.Collection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
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

    /** The public id of the token the user signed in with, or tried to. */
    private final @Nullable String publicId;

    public PatAuthenticationToken(Object principal,
                                  Object credentials,
                                  Collection<? extends GrantedAuthority> authorities,
                                  String publicId) {
        super(principal, credentials, authorities);
        this.publicId = publicId;
    }

    /**
     * Creates the token of a sign-in attempt that was refused. It is not authenticated, and names the owner of the
     * token as its user when the token is known.
     *
     * @param publicId the public id of the token presented, or {@code null} when the token could not be read
     * @param owner    the login name of the owner of the token, or {@code null} when the token is not known
     */
    public PatAuthenticationToken(@Nullable String publicId, @Nullable String owner) {
        super(owner, null);
        this.publicId = publicId;
    }
}
