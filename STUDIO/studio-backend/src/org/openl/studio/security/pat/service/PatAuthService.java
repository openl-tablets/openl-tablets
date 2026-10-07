package org.openl.studio.security.pat.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatToken;

/**
 * Service for resolving Personal Access Token authentication.
 * <p>
 * This service validates PATs and converts valid tokens into Spring Security
 * authentication objects that can be used in the security context.
 * </p>
 *
 * @since 6.0.0
 */
public interface PatAuthService {

    /**
     * Resolves authentication from a Personal Access Token.
     * <p>
     * This method validates the token and, if valid, creates a Spring Security
     * authentication object with the user's credentials and authorities.
     * </p>
     *
     * @param pat the Personal Access Token to authenticate (must not be null)
     * @return the authentication resolution containing validation status and authentication object
     */
    PatAuthResolution resolveAuthentication(@Valid @NotNull PatToken pat);

}
