package org.openl.studio.socket.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextHandshakeInterceptorTest {

    private final SecurityContextHandshakeInterceptor interceptor = new SecurityContextHandshakeInterceptor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesHeaderAuthenticatedContextNotPresentInSession() {
        var auth = new UsernamePasswordAuthenticationToken("third-party", "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(null, null, null, attributes));

        assertSame(SecurityContextHolder.getContext(), attributes.get(SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void storesAnonymousContext() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        Map<String, Object> attributes = new HashMap<>();

        interceptor.beforeHandshake(null, null, null, attributes);

        assertTrue(attributes.containsKey(SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void doesNotOverrideContextProvidedBySession() {
        var sessionContext = SecurityContextHolder.createEmptyContext();
        sessionContext.setAuthentication(new UsernamePasswordAuthenticationToken("session-user", "n/a", List.of()));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SPRING_SECURITY_CONTEXT_KEY, sessionContext);

        // A different context is in the holder; the session-provided one must win.
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("other", "n/a", List.of()));

        interceptor.beforeHandshake(null, null, null, attributes);

        assertSame(sessionContext, attributes.get(SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void skipsWhenNoAuthentication() {
        Map<String, Object> attributes = new HashMap<>();

        interceptor.beforeHandshake(null, null, null, attributes);

        assertFalse(attributes.containsKey(SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void skipsWhenAuthenticationNotAuthenticated() {
        // Two-argument constructor produces a not-yet-authenticated token.
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "pw"));
        Map<String, Object> attributes = new HashMap<>();

        interceptor.beforeHandshake(null, null, null, attributes);

        assertFalse(attributes.containsKey(SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void storedContextCarriesAuthentication() {
        var auth = new UsernamePasswordAuthenticationToken("third-party", "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        Map<String, Object> attributes = new HashMap<>();

        interceptor.beforeHandshake(null, null, null, attributes);

        var stored = (SecurityContext) attributes.get(SPRING_SECURITY_CONTEXT_KEY);
        assertEquals("third-party", stored.getAuthentication().getName());
    }
}
