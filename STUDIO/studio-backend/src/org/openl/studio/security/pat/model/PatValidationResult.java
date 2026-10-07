package org.openl.studio.security.pat.model;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;

/**
 * Result of Personal Access Token validation.
 * <p>
 * Contains the validation status and the stored token entity if the token is valid.
 * This is an intermediate result used before converting to authentication.
 * </p>
 *
 * @param valid the validation status of the PAT
 * @param token  the stored token entity from database (null if validation failed)
 * @since 6.0.0
 */
public record PatValidationResult(
        boolean valid,
        PersonalAccessToken token
) {

    private static final PatValidationResult INVALID_RESULT = new PatValidationResult(false, null);
    /**
     * Creates a successful validation result with the stored token entity.
     *
     * @param token the valid token entity from database
     * @return a validation result with VALID status
     */
    public static PatValidationResult valid(PersonalAccessToken token) {
        return new PatValidationResult(true, token);
    }

    /**
     * Creates a failed validation result.
     *
     * @return a validation result with null token
     */
    public static PatValidationResult invalid() {
        return INVALID_RESULT;
    }
}
