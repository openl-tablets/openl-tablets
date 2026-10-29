package org.openl.studio.security.pat.service;

import java.time.Clock;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.security.standalone.dao.PersonalAccessTokenDao;
import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.ExpiredPatException;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.PatValidationResult;
import org.openl.studio.security.pat.model.UnknownPatException;

/**
 * Default implementation of {@link PatValidationService}.
 * <p>
 * This service validates Personal Access Tokens by:
 * <ul>
 *   <li>Checking token existence in the database</li>
 *   <li>Verifying the secret using secure password matching</li>
 *   <li>Checking expiration status against current time</li>
 * </ul>
 * </p>
 *
 * @since 6.0.0
 */
@Validated
public class PatValidationServiceImpl implements PatValidationService {

    private final PersonalAccessTokenDao tokenDao;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String dummyHash;

    /**
     * Constructs a new PatValidationServiceImpl.
     * <p>
     * Generates a dummy hash during construction to ensure it's valid for the configured
     * password encoder. This dummy hash is used for timing attack prevention when validating
     * non-existent tokens.
     * </p>
     *
     * @param tokenDao        the DAO for accessing stored tokens
     * @param passwordEncoder the password encoder for verifying secrets
     * @param clock           the clock for checking expiration
     */
    public PatValidationServiceImpl(PersonalAccessTokenDao tokenDao,
                                    PasswordEncoder passwordEncoder,
                                    Clock clock) {
        this.tokenDao = tokenDao;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;

        // Generate a valid dummy hash using the actual password encoder
        // This ensures timing consistency regardless of the encoder implementation
        this.dummyHash = passwordEncoder.encode("$2a$10$dummyHashToPreventTimingAttack1234567890123456789012");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs the following security checks:
     * <ul>
     *   <li>Token existence verification</li>
     *   <li>Constant-time secret comparison to prevent timing attacks</li>
     *   <li>Token expiration check</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *   <li>Names the reason of a refusal for the audit log: an unknown token, a wrong secret, an expired token.
     *       The expiry is checked only once the secret matched, so it tells nothing to someone without the secret</li>
     *   <li>Uses constant-time password comparison to prevent timing attacks</li>
     *   <li>Leaves a failure to read the token, such as a database error, to the caller, which answers it as a
     *       failure of the server without its details</li>
     *   <li>Uses dummy hash for non-existent tokens to maintain consistent timing</li>
     * </ul>
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public PatValidationResult validate(@Valid @NotNull PatToken pat) {
        var stored = tokenDao.getByPublicId(pat.publicId());
        // Checked for a token that does not exist too, so the time of the answer tells nothing
        var secretMatches = secretMatches(pat, stored);

        if (stored == null) {
            return PatValidationResult.refused(new UnknownPatException(), null);
        }

        // If secret doesn't match, return without checking the expiration status
        if (!secretMatches) {
            return PatValidationResult.refused(new BadCredentialsException("Wrong secret of the token"),
                    stored.getLoginName());
        }

        if (isExpired(stored)) {
            return PatValidationResult.refused(new ExpiredPatException(), stored.getLoginName());
        }

        return PatValidationResult.valid(stored);
    }

    /**
     * Checks the secret of the token against the stored hash.
     * <p>
     * An error of the password encoder counts as a mismatch.
     */
    private boolean secretMatches(PatToken pat, @Nullable PersonalAccessToken stored) {
        // Password encoder may throw exceptions (invalid hash format, etc.)
        // We catch these to prevent information leakage
        try {
            // Always perform password check to prevent timing attacks
            // Use the pre-generated dummy hash if token doesn't exist to maintain consistent timing
            String hashToCheck = stored != null ? stored.getSecretHash() : dummyHash;
            return passwordEncoder.matches(pat.secret(), hashToCheck);
        } catch (Exception e) {
            // Invalid hash format or other encoder error - treat as invalid
            return false;
        }
    }

    /**
     * Checks if the token has expired.
     *
     * @param token the token to check
     * @return true if the token has an expiration date and it's in the past, false otherwise
     */
    private boolean isExpired(PersonalAccessToken token) {
        var expiresAt = token.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(Instant.now(clock));
    }
}
