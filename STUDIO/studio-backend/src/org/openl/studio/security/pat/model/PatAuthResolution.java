package org.openl.studio.security.pat.model;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Result of Personal Access Token authentication resolution.
 * <p>
 * Contains the authenticated user's credentials if the token is valid, or why it was refused.
 * This model bridges PAT validation with Spring Security authentication.
 * </p>
 *
 * @param authentication the Spring Security authentication object, or {@code null} when the token was refused
 * @param refusal        why the token was refused, or {@code null} when it is valid
 * @since 6.0.0
 */
public record PatAuthResolution(@Nullable Authentication authentication, @Nullable PatRefusal refusal) {

    /**
     * Creates a successful authentication resolution with the provided authentication.
     *
     * @param auth the Spring Security authentication object
     * @return an authentication resolution of a valid token
     */
    public static PatAuthResolution valid(Authentication auth) {
        return new PatAuthResolution(auth, null);
    }

    /**
     * Creates a failed authentication resolution.
     *
     * @param refusal why the token was refused
     * @return an authentication resolution without authentication
     */
    public static PatAuthResolution refused(PatRefusal refusal) {
        return new PatAuthResolution(null, refusal);
    }

    /** Whether the token is valid. */
    public boolean valid() {
        return refusal == null;
    }
}
