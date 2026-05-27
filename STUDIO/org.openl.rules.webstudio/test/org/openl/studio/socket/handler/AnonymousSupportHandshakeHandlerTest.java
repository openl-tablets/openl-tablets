package org.openl.studio.socket.handler;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AnonymousSupportHandshakeHandlerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void prefersServletPrincipalWhenPresent() {
        Principal servletPrincipal = new UsernamePasswordAuthenticationToken("session-user", "n/a", List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("other", "n/a", List.of()));

        assertSame(servletPrincipal, AnonymousSupportHandshakeHandler.resolveUser(servletPrincipal));
    }

    @Test
    void fallsBackToHeaderAuthenticatedContext() {
        var auth = new UsernamePasswordAuthenticationToken("third-party", "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertSame(auth, AnonymousSupportHandshakeHandler.resolveUser(null));
    }

    @Test
    void fallsBackToAnonymousContext() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertSame(anonymous, AnonymousSupportHandshakeHandler.resolveUser(null));
    }

    @Test
    void returnsNullWhenNoPrincipalAndNoAuthentication() {
        assertNull(AnonymousSupportHandshakeHandler.resolveUser(null));
    }

    @Test
    void returnsNullWhenNoPrincipalAndUnauthenticatedToken() {
        // Two-argument constructor produces a not-yet-authenticated token.
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "pw"));

        assertNull(AnonymousSupportHandshakeHandler.resolveUser(null));
    }
}
