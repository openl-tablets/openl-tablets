package org.openl.studio.security.pat.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.security.pat.service.PatAuthService;

/**
 * Authentication filter for Personal Access Tokens (PAT).
 * <p>
 * This filter processes requests with the "{@code HttpHeaders.AUTHORIZATION}: Token &lt;pat&gt;" header
 * for service-to-service authorization in every authenticated user mode (OAuth2, SAML, AD and multi).
 * </p>
 * <p>
 * The filter extracts the PAT token, validates it using {@link PatAuthService},
 * and sets the authentication in the {@link SecurityContext} if valid.
 * </p>
 * <p>
 * This filter should be placed before the credential authentication filter (bearer or basic)
 * in the security filter chain.
 * </p>
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PatAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Token ";

    private final PatAuthService patAuthService;
    private final AuthenticationEventPublisher eventPublisher;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    /**
     * Constructs a new PatAuthenticationFilter with default SecurityContextHolderStrategy.
     *
     * @param patAuthService the PAT authentication service
     * @param eventPublisher publishes every refused token, which the audit log records
     */
    public PatAuthenticationFilter(PatAuthService patAuthService, AuthenticationEventPublisher eventPublisher) {
        this(patAuthService, eventPublisher, SecurityContextHolder.getContextHolderStrategy());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        var tokenValue = header.substring(PREFIX.length()).trim();

        PatToken patToken;
        try {
            patToken = PatToken.parse(tokenValue);
        } catch (IllegalArgumentException e) {
            refuse(null, response);
            return;
        }

        var resolution = patAuthService.resolveAuthentication(patToken);

        if (!resolution.valid()) {
            refuse(patToken.publicId(), response);
            return;
        }

        var authResult = resolution.authentication();

        if (authenticationIsRequired(authResult.getName())) {
            var context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authResult);
            securityContextHolderStrategy.setContext(context);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Answers {@code 401} to a token that was refused, and publishes the refusal without telling why.
     *
     * @param publicId the public id of the token, or {@code null} when the token could not be read
     */
    private void refuse(@Nullable String publicId, HttpServletResponse response) throws IOException {
        eventPublisher.publishAuthenticationFailure(new BadCredentialsException("Invalid personal access token"),
                new PatAuthenticationToken(publicId));
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase());
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
        var existingAuth = securityContextHolderStrategy.getContext().getAuthentication();

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
