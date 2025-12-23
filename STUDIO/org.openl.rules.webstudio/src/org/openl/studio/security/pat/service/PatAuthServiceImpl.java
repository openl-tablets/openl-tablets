package org.openl.studio.security.pat.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.model.PatValidationResult;

/**
 * Default implementation of {@link PatAuthService}.
 * <p>
 * This service validates Personal Access Tokens and creates Spring Security authentication
 * objects for valid tokens by loading user details and mapping external groups.
 * </p>
 *
 * @since 6.0.0
 */
@Validated
public class PatAuthServiceImpl implements PatAuthService {

    private final PatValidationService validator;
    private final UserDetailsService userDetailsService;

    /**
     * Constructs a new PatAuthServiceImpl.
     *
     * @param validator          the PAT validation service
     * @param userDetailsService the user details service for loading user credentials
     */
    public PatAuthServiceImpl(PatValidationService validator,
                              UserDetailsService userDetailsService) {
        this.validator = validator;
        this.userDetailsService = userDetailsService;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation validates the token and loads the user details to create
     * a {@link UsernamePasswordAuthenticationToken} with the user's authorities.
     * </p>
     * <p>
     * <strong>Security Note:</strong> This method also verifies that the user account
     * is enabled, not locked, and not expired before granting authentication.
     * All validation failures return INVALID status without revealing the specific reason.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public PatAuthResolution resolveAuthentication(@Valid @NotNull PatToken pat) {
        PatValidationResult result = validator.validate(pat);
        if (!result.valid()) {
            return PatAuthResolution.invalid();
        }

        PersonalAccessToken token = result.token();
        UserDetails user = userDetailsService.loadUserByUsername(token.getLoginName());

        // Security check: verify user account is enabled and not locked
        // Return INVALID for all failures to prevent information disclosure
        if (!user.isEnabled() || !user.isAccountNonLocked()
                || !user.isAccountNonExpired() || !user.isCredentialsNonExpired()) {
            return PatAuthResolution.invalid();
        }
        var auth = new PatAuthenticationToken(user, null, user.getAuthorities());

        return PatAuthResolution.valid(auth);
    }

}
