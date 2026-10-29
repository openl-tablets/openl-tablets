package org.openl.studio.security.pat.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import org.openl.studio.common.model.BaseError;
import org.openl.studio.security.pat.model.ExpiredPatException;
import org.openl.studio.security.pat.model.MalformedPatException;
import org.openl.studio.security.pat.model.PatAuthResolution;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.security.pat.model.PatRefusal;
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
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PatAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Token ";
    /** The prefix of the error codes of a refused token, by their status. */
    private static final String ERROR_PREFIX = "openl.error.401.";

    private final PatAuthService patAuthService;
    private final AuthenticationEventPublisher eventPublisher;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    /**
     * Constructs a new PatAuthenticationFilter with default SecurityContextHolderStrategy.
     *
     * @param patAuthService the PAT authentication service
     * @param eventPublisher publishes every refused token, which the audit log records
     * @param messageSource  the messages of the answer to a refused token
     * @param objectMapper   writes the answer to a refused token
     */
    public PatAuthenticationFilter(PatAuthService patAuthService,
                                   AuthenticationEventPublisher eventPublisher,
                                   MessageSource messageSource,
                                   ObjectMapper objectMapper) {
        this(patAuthService, eventPublisher, messageSource, objectMapper,
                SecurityContextHolder.getContextHolderStrategy());
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
            refuse(null, new PatRefusal(new MalformedPatException(), null), response);
            return;
        }

        PatAuthResolution resolution;
        try {
            resolution = patAuthService.resolveAuthentication(patToken);
        } catch (RuntimeException e) {
            fail(patToken.publicId(), e, response);
            return;
        }

        if (resolution.refusal() instanceof PatRefusal refusal) {
            refuse(patToken.publicId(), refusal, response);
            return;
        }

        var authResult = Objects.requireNonNull(resolution.authentication());

        if (authenticationIsRequired(authResult.getName())) {
            var context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authResult);
            securityContextHolderStrategy.setContext(context);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Answers {@code 401} to a token that was refused, and publishes the refusal with its reason and the owner of the
     * token, which the audit log records.
     *
     * <p>The answer tells what to do about the token, in its JSON body and in the {@code WWW-Authenticate} header, so
     * a client such as an MCP client shows it rather than offering another way to sign in. It names the exact reason
     * only once the secret matched: the token expired, or its owner cannot sign in. Any other token gets one common
     * answer, which tells nothing to someone who does not hold the token.
     *
     * @param publicId the public id of the token, or {@code null} when the token could not be read
     */
    private void refuse(@Nullable String publicId, PatRefusal refusal, HttpServletResponse response)
            throws IOException {
        eventPublisher.publishAuthenticationFailure(refusal.reason(),
                new PatAuthenticationToken(publicId, refusal.owner()));
        var code = ERROR_PREFIX + messageKeyOf(refusal.reason());
        var message = messageSource.getMessage(code, null, Locale.US);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Token error=\"invalid_token\", error_description=\"" + message + "\"");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), BaseError.builder().code(code).message(message).build());
    }

    /**
     * Answers {@code 500} to a token that could not be checked, as when the database cannot be read: the server
     * failed, not the token, so nothing tells to replace it. The failure is logged, and published for the audit log.
     *
     * @param publicId the public id of the token
     */
    private void fail(String publicId, RuntimeException failure, HttpServletResponse response) {
        log.error("Failed to check a personal access token.", failure);
        eventPublisher.publishAuthenticationFailure(
                new AuthenticationServiceException("The personal access token cannot be checked", failure),
                new PatAuthenticationToken(publicId, null));
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /** The message of the answer to a refused token: the exact reason only once the secret matched. */
    private static String messageKeyOf(AuthenticationException reason) {
        return switch (reason) {
            case ExpiredPatException ignored -> "token.expired.message";
            case DisabledException ignored -> "token.user.disabled.message";
            case LockedException ignored -> "token.user.locked.message";
            case AccountStatusException ignored -> "token.user.expired.message";
            default -> "token.refused.message";
        };
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
