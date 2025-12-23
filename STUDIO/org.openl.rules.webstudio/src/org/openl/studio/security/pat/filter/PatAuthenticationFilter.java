package org.openl.studio.security.pat.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.service.PatAuthService;

/**
 * Authentication filter for Personal Access Tokens (PAT).
 * <p>
 * This filter processes requests with the "{@code HttpHeaders.AUTHORIZATION}: Token &lt;pat&gt;" header
 * for service-to-service authorization in OAuth2 environments.
 * </p>
 * <p>
 * The filter extracts the PAT token, validates it using {@link PatAuthService},
 * and sets the authentication in the {@link SecurityContext} if valid.
 * </p>
 * <p>
 * This filter should be placed before the bearer token authentication filter
 * in the security filter chain.
 * </p>
 */
public class PatAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Token ";

    private final PatAuthService patAuthService;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    /**
     * Constructs a new PatAuthenticationFilter with default SecurityContextHolderStrategy.
     *
     * @param patAuthService the PAT authentication service
     */
    public PatAuthenticationFilter(PatAuthService patAuthService) {
        this.patAuthService = patAuthService;
        this.securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    }

    /**
     * Package-private constructor for testing with custom SecurityContextHolderStrategy.
     *
     * @param patAuthService                the PAT authentication service
     * @param securityContextHolderStrategy the security context holder strategy
     */
    PatAuthenticationFilter(PatAuthService patAuthService, SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.patAuthService = patAuthService;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenValue = header.substring(PREFIX.length()).trim();

        PatToken patToken;
        try {
            patToken = PatToken.parse(tokenValue);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase());
            return;
        }

        PatAuthResolution resolution = patAuthService.resolveAuthentication(patToken);

        if (!resolution.valid()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase());
            return;
        }

        Authentication authResult = resolution.authentication();

        if (authenticationIsRequired(authResult.getName())) {
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authResult);
            securityContextHolderStrategy.setContext(context);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if authentication should be set in the security context.
     * <p>
     * Authentication is required if:
     * <ul>
     *   <li>No authentication exists in the current context</li>
     *   <li>Existing authentication is not authenticated</li>
     *   <li>Existing authentication is anonymous</li>
     *   <li>Existing authentication is for a different user</li>
     * </ul>
     * </p>
     *
     * @param username the username from the PAT
     * @return true if authentication should be set, false otherwise
     */
    protected boolean authenticationIsRequired(String username) {
        Authentication existingAuth = securityContextHolderStrategy.getContext().getAuthentication();

        if (existingAuth == null) {
            return true;
        }
        if (!existingAuth.isAuthenticated()) {
            return true;
        }
        if (existingAuth instanceof AnonymousAuthenticationToken) {
            return true;
        }
        return !username.equals(existingAuth.getName());
    }
}
