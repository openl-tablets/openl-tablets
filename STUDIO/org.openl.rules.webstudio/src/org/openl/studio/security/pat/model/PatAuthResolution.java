package org.openl.studio.security.pat.model;

import org.springframework.security.core.Authentication;

/**
 * Result of Personal Access Token authentication resolution.
 * <p>
 * Contains the validation status and the authenticated user's credentials if the token is valid.
 * This model bridges PAT validation with Spring Security authentication.
 * </p>
 *
 * @param valid         the validation status of the PAT (never null)
 * @param authentication the Spring Security authentication object (null if token is invalid)
 * @since 6.0.0
 */
public record PatAuthResolution(
        boolean valid,

        Authentication authentication
) {

    private static final PatAuthResolution INVALID_RESOLUTION = new PatAuthResolution(false, null);

    /**
     * Creates a successful authentication resolution with the provided authentication.
     *
     * @param auth the Spring Security authentication object
     * @return an authentication resolution with VALID status
     */
    public static PatAuthResolution valid(Authentication auth) {
        return new PatAuthResolution(true, auth);
    }

    /**
     * Creates a failed authentication resolution.
     *
     * @return an authentication resolution with null authentication
     */
    public static PatAuthResolution invalid() {
        return INVALID_RESOLUTION;
    }
}
