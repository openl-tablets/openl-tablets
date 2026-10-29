package org.openl.studio.security.pat.service;

import java.util.Objects;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.security.pat.model.PatRefusal;
import org.openl.studio.security.pat.model.PatToken;

/**
 * Default implementation of {@link PatAuthService}.
 * <p>
 * This service validates Personal Access Tokens and creates Spring Security authentication
 * objects for valid tokens by loading user details and mapping external groups.
 * </p>
 *
 * @since 6.0.0
 */
@RequiredArgsConstructor
@Validated
public class PatAuthServiceImpl implements PatAuthService {

    private static final AccountStatusUserDetailsChecker USER_CHECKER = new AccountStatusUserDetailsChecker();

    private final PatValidationService validator;
    private final UserDetailsService userDetailsService;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation validates the token and loads the user details to create
     * a {@link UsernamePasswordAuthenticationToken} with the user's authorities.
     * </p>
     * <p>
     * <strong>Security Note:</strong> This method also verifies that the user account
     * is enabled, not locked, and not expired before granting authentication.
     * A refusal names its reason, for the audit log and for the caller who sent the right secret.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public PatAuthResolution resolveAuthentication(@Valid @NotNull PatToken pat) {
        var result = validator.validate(pat);
        if (result.refusal() instanceof PatRefusal refusal) {
            return PatAuthResolution.refused(refusal);
        }

        var owner = Objects.requireNonNull(result.token()).getLoginName();
        try {
            var user = userDetailsService.loadUserByUsername(owner);
            // Security check: the user account is enabled, not locked and not expired
            USER_CHECKER.check(user);
            return PatAuthResolution.valid(new PatAuthenticationToken(user, null, user.getAuthorities(),
                    pat.publicId()));
        } catch (AuthenticationException e) {
            // The owner is not found, or cannot sign in. A failure to read the owner is left to the caller, as the
            // transaction it failed in cannot be committed.
            return PatAuthResolution.refused(new PatRefusal(e, owner));
        }
    }

}
