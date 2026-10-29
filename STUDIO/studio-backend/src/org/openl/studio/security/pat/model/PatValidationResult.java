package org.openl.studio.security.pat.model;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;

/**
 * Result of Personal Access Token validation.
 * <p>
 * Contains the stored token entity if the token is valid, or why it was refused.
 * This is an intermediate result used before converting to authentication.
 * </p>
 *
 * @param token   the stored token entity from database, or {@code null} when the token was refused
 * @param refusal why the token was refused, or {@code null} when it is valid
 * @since 6.0.0
 */
public record PatValidationResult(@Nullable PersonalAccessToken token, @Nullable PatRefusal refusal) {

    /**
     * Creates a successful validation result with the stored token entity.
     *
     * @param token the valid token entity from database
     * @return a validation result of a valid token
     */
    public static PatValidationResult valid(PersonalAccessToken token) {
        return new PatValidationResult(token, null);
    }

    /**
     * Creates a failed validation result.
     *
     * @param reason why the token was refused
     * @param owner  the login name of the owner of the token, or {@code null} when the token is not known
     * @return a validation result without a token
     */
    public static PatValidationResult refused(AuthenticationException reason, @Nullable String owner) {
        return new PatValidationResult(null, new PatRefusal(reason, owner));
    }

    /** Whether the token is valid. */
    public boolean valid() {
        return refusal == null;
    }
}
