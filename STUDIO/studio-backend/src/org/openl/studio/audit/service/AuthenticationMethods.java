package org.openl.studio.audit.service;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.security.pat.model.PatAuthenticationToken;

/**
 * Tells how a user signed in, or tried to, from the authentication Spring Security holds for the user.
 *
 * <p>A personal access token, a bearer token, OpenID Connect and SAML each have an authentication of their own. A user
 * name and a password come the same way from the login form and from Basic authentication, so the request tells them
 * apart: a Basic {@code Authorization} header sends the password with the request itself.
 *
 * <p>Outside a request, as in a merge running in a thread of its own, a user name and a password tell nothing, and so
 * does an authentication of any other kind.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AuthenticationMethods {

    private static final String BASIC_PREFIX = "basic ";

    /**
     * How the user of an authentication signed in, read against the current request.
     */
    static @Nullable AuditAuthentication of(@Nullable Authentication authentication) {
        return switch (authentication) {
            case null -> null;
            case PatAuthenticationToken ignored -> AuditAuthentication.TOKEN;
            case BearerTokenAuthenticationToken ignored -> AuditAuthentication.BEARER;
            case AbstractOAuth2TokenAuthenticationToken<?> ignored -> AuditAuthentication.BEARER;
            case OAuth2LoginAuthenticationToken ignored -> AuditAuthentication.OAUTH2;
            case OAuth2AuthenticationToken ignored -> AuditAuthentication.OAUTH2;
            case Saml2AuthenticationToken ignored -> AuditAuthentication.SAML;
            case Saml2Authentication ignored -> AuditAuthentication.SAML;
            case UsernamePasswordAuthenticationToken ignored -> passwordAuthentication();
            default -> null;
        };
    }

    /**
     * How the user of a browser session signed in. A browser signs in with a password through the login form only.
     */
    static @Nullable AuditAuthentication ofSession(Authentication authentication) {
        return switch (authentication) {
            case PatAuthenticationToken ignored -> AuditAuthentication.TOKEN;
            case UsernamePasswordAuthenticationToken ignored -> AuditAuthentication.FORM;
            default -> of(authentication);
        };
    }

    private static @Nullable AuditAuthentication passwordAuthentication() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return isBasic(attributes.getRequest()) ? AuditAuthentication.BASIC : AuditAuthentication.FORM;
    }

    private static boolean isBasic(HttpServletRequest request) {
        var header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length());
    }
}
