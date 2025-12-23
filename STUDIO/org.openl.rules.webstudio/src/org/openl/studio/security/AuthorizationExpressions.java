package org.openl.studio.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import org.openl.studio.security.pat.model.PatAuthenticationToken;

/**
 * Authorization expressions for Personal Access Token (PAT) authentication.
 * <p>
 * This component provides custom authorization checks related to PATs
 * that can be used in security expressions.
 * </p>
 */
@Component("authz")
public class AuthorizationExpressions {

    /**
     * Checks if the current authentication is not a Personal Access Token (PAT) authentication.
     *
     * @param authentication the current authentication object
     * @return true if the authentication is not a PAT authentication, false otherwise
     */
    public boolean isNotPat(Authentication authentication) {
        return !(authentication instanceof PatAuthenticationToken);
    }
}
