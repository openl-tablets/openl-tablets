package org.openl.studio.security.pat.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.PatValidationResult;

/**
 * Service for validating Personal Access Tokens (PATs).
 * <p>
 * This service performs cryptographic validation of PATs by checking:
 * <ul>
 *   <li>Token existence in the database</li>
 *   <li>Secret hash matching using secure password encoder</li>
 *   <li>Token expiration status</li>
 * </ul>
 * </p>
 *
 * @since 6.0.0
 */
public interface PatValidationService {

    /**
     * Validates a Personal Access Token.
     * <p>
     * The method checks if the token exists, verifies the secret hash matches,
     * and ensures the token has not expired. Returns the validation result
     * with the stored token entity if validation succeeds.
     * </p>
     *
     * @param pat the Personal Access Token to validate (must not be null)
     * @return the validation result containing status and stored token entity if valid
     */
    PatValidationResult validate(@Valid @NotNull PatToken pat);
}
