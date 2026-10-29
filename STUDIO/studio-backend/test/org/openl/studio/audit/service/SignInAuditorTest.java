package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.session.ClientSessions;
import org.openl.studio.session.CredentialClient;

class SignInAuditorTest {

    private final ClientSessions clientSessions = mock(ClientSessions.class);
    private AuditService auditService;
    private SignInAuditor auditor;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        when(auditService.isEnabled()).thenReturn(true);
        auditor = new SignInAuditor(auditService, clientSessions);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsASignInThroughTheLoginForm() {
        requestTo("/login");

        auditor.onSignIn(new InteractiveAuthenticationSuccessEvent(signedIn("jdoe"), getClass()));

        var event = recorded();
        assertEquals(AuditAction.SIGN_IN, event.action());
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.FORM, event.authentication());
        assertEquals(Map.of(), event.details());
    }

    @Test
    void recordsABasicSignInRefusedWithTheTypedName() {
        requestTo("/rest/projects").addHeader(HttpHeaders.AUTHORIZATION, "Basic amRvZTp3cm9uZw==");
        var attempt = UsernamePasswordAuthenticationToken.unauthenticated("jdoe", "wrong");

        auditor.onSignInFailure(new AuthenticationFailureBadCredentialsEvent(attempt,
                new BadCredentialsException("Bad credentials")));

        var event = recorded();
        assertEquals(AuditAction.SIGN_IN, event.action());
        assertEquals(AuditOutcome.FAILURE, event.outcome());
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.BASIC, event.authentication());
        assertEquals(Map.of(SignInAuditor.REASON, "badCredentials"), event.details());
    }

    @Test
    void neverRecordsARefusedTokenAsTheName() {
        // The name of a bearer token that was not read yet is the token itself.
        var attempt = new BearerTokenAuthenticationToken("secret-token-value");

        auditor.onSignInFailure(new AuthenticationFailureBadCredentialsEvent(attempt,
                new InvalidBearerTokenException("Invalid token")));

        var event = recorded();
        assertNull(event.actor());
        assertEquals(AuditAuthentication.BEARER, event.authentication());
        assertEquals(Map.of(SignInAuditor.REASON, "invalidToken"), event.details());
    }

    @Test
    void namesARefusedPersonalAccessTokenByItsPublicId() {
        auditor.onSignInFailure(new AuthenticationFailureBadCredentialsEvent(
                new PatAuthenticationToken("abc123DEF4567890"),
                new BadCredentialsException("Invalid personal access token")));
        auditor.onSignInFailure(new AuthenticationFailureBadCredentialsEvent(
                new PatAuthenticationToken(null),
                new BadCredentialsException("Invalid personal access token")));

        var events = recordedAll();
        assertNull(events.getFirst().actor());
        assertEquals(AuditAuthentication.TOKEN, events.getFirst().authentication());
        assertEquals(Map.of(SignInAuditor.REASON, "badCredentials", SignInAuditor.TOKEN, "abc123DEF4567890"),
                events.getFirst().details());
        assertEquals(Map.of(SignInAuditor.REASON, "badCredentials"), events.get(1).details());
    }

    @Test
    void namesNoUserWhenNoNameWasTyped() {
        var attempt = UsernamePasswordAuthenticationToken.unauthenticated(" ", "");

        auditor.onSignInFailure(new AuthenticationFailureBadCredentialsEvent(attempt,
                new BadCredentialsException("Bad credentials")));

        assertNull(recorded().actor());
    }

    @Test
    void recordsASignOut() {
        requestTo("/logout");

        auditor.onSignOut(new LogoutSuccessEvent(signedIn("jdoe")));

        var event = recorded();
        assertEquals(AuditAction.SIGN_OUT, event.action());
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.FORM, event.authentication());
    }

    @Test
    void opensTheClientOfARequestSoThatItsStartIsHeard() {
        assertTrue(auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object()));

        verify(clientSessions).open();
    }

    @Test
    void recordsTheStartAndTheEndsOfTheSessionOfAClientOfCredentials() {
        var client = new CredentialClient("jdoe", signedIn("jdoe"));

        auditor.started(client);
        auditor.timedOut(client);
        auditor.restarted(List.of(client));

        var events = recordedAll();
        assertEquals(List.of(AuditAction.SESSION_STARTED, AuditAction.SESSION_ENDED, AuditAction.SESSION_ENDED),
                events.stream().map(AuditEvent::action).toList());
        assertEquals(List.of(Map.of(), Map.of(SignInAuditor.REASON, SignInAuditor.TIMED_OUT),
                        Map.of(SignInAuditor.REASON, SignInAuditor.RESTARTED)),
                events.stream().map(AuditEvent::details).toList());
        events.forEach(event -> {
            assertEquals("jdoe", event.actor());
            assertEquals(AuditOutcome.SUCCESS, event.outcome());
            assertEquals(AuditAuthentication.BASIC, event.authentication(), "Told without a request");
        });
    }

    @Test
    void namesThePersonalAccessTokenOfAClient() {
        var client = new CredentialClient("jdoe|pat:abc123DEF4567890",
                new PatAuthenticationToken("jdoe", null, List.of(), "abc123DEF4567890"));

        auditor.started(client);
        auditor.timedOut(client);

        var events = recordedAll();
        assertEquals(AuditAuthentication.TOKEN, events.getFirst().authentication());
        assertEquals(Map.of(SignInAuditor.TOKEN, "abc123DEF4567890"), events.getFirst().details());
        assertEquals(Map.of(SignInAuditor.REASON, SignInAuditor.TIMED_OUT, SignInAuditor.TOKEN, "abc123DEF4567890"),
                events.getLast().details());
    }

    @Test
    void opensAndRecordsNoClientWhileTheLogIsOff() {
        when(auditService.isEnabled()).thenReturn(false);
        var client = new CredentialClient("jdoe", signedIn("jdoe"));

        auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());
        auditor.started(client);
        auditor.timedOut(client);
        auditor.restarted(List.of(client));

        verify(clientSessions, never()).open();
        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void recordsTheEndOfASessionLeftIdle() {
        var now = System.currentTimeMillis();
        auditor.onSessionEnd(new HttpSessionDestroyedEvent(sessionOf(signedIn("jdoe"), now - 1_801_000)));
        auditor.onSessionEnd(new HttpSessionDestroyedEvent(sessionOf(signedIn("jsmith"), now)));

        var event = recorded();
        assertEquals(AuditAction.SESSION_ENDED, event.action());
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.FORM, event.authentication());
        assertEquals(Map.of(SignInAuditor.REASON, SignInAuditor.TIMED_OUT), event.details());
    }

    @Test
    void recordsTheEndOfASessionARestartEnded() {
        var ended = new MockHttpSession();
        ended.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(signedIn("jdoe")));
        SignInAuditor.endedByRestart(ended);
        var signedOut = new MockHttpSession();
        signedOut.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(signedIn("jsmith")));

        auditor.onSessionEnd(new HttpSessionDestroyedEvent(ended));
        auditor.onSessionEnd(new HttpSessionDestroyedEvent(signedOut));

        var event = recorded();
        assertEquals(AuditAction.SESSION_ENDED, event.action());
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.FORM, event.authentication());
        assertEquals(Map.of(SignInAuditor.REASON, SignInAuditor.RESTARTED), event.details());
    }

    @Test
    void recordsNothingForASessionThatTellsNothingMore() {
        var session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenThrow(new IllegalStateException("invalidated"));

        auditor.onSessionEnd(new HttpSessionDestroyedEvent(session));

        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void tellsASessionLeftIdleFromOneEndedOtherwise() {
        var session = mock(HttpSession.class);
        when(session.getMaxInactiveInterval()).thenReturn(60);
        when(session.getLastAccessedTime()).thenReturn(1_000L);

        assertTrue(SignInAuditor.timedOut(session, 61_000));
        assertFalse(SignInAuditor.timedOut(session, 60_999));
        when(session.getMaxInactiveInterval()).thenReturn(0);
        assertFalse(SignInAuditor.timedOut(session, 61_000), "A session that never times out");
        when(session.getMaxInactiveInterval()).thenThrow(new IllegalStateException("invalidated"));
        assertFalse(SignInAuditor.timedOut(session, 61_000), "A session that tells nothing more");
    }

    @Test
    void neverFailsASignInForTheAuditLog() {
        doThrow(new IllegalStateException("The database is down")).when(auditService).recordEvent(any());

        assertDoesNotThrow(() -> auditor.onSignIn(new InteractiveAuthenticationSuccessEvent(signedIn("jdoe"),
                getClass())));
    }

    @Test
    void tellsWhyASignInWasRefused() {
        assertEquals("badCredentials", SignInAuditor.reasonOf(new UsernameNotFoundException("jdoe")));
        assertEquals("locked", SignInAuditor.reasonOf(new LockedException("locked")));
        assertEquals("disabled", SignInAuditor.reasonOf(new DisabledException("disabled")));
        assertEquals("expired", SignInAuditor.reasonOf(new AccountExpiredException("expired")));
        assertEquals("expired", SignInAuditor.reasonOf(new CredentialsExpiredException("expired")));
        assertEquals("error", SignInAuditor.reasonOf(new AuthenticationServiceException("down")));
    }

    private static Authentication signedIn(String name) {
        return UsernamePasswordAuthenticationToken.authenticated(name, null, List.of());
    }

    /** A request of a user with Basic authentication, signed in for the request alone. */
    private static MockHttpServletRequest basicRequest(String user) {
        var request = requestTo("/rest/projects");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + user);
        SecurityContextHolder.getContext().setAuthentication(signedIn(user));
        return request;
    }

    /** A session a user signed in to, last used at the given time and idle for half an hour at most. */
    private static HttpSession sessionOf(Authentication authentication, long lastAccessedTime) {
        var session = mock(HttpSession.class);
        when(session.getMaxInactiveInterval()).thenReturn(1800);
        when(session.getLastAccessedTime()).thenReturn(lastAccessedTime);
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(
                List.of(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)));
        when(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .thenReturn(new SecurityContextImpl(authentication));
        return session;
    }

    private static MockHttpServletRequest requestTo(String path) {
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private AuditEvent recorded() {
        var events = recordedAll();
        assertEquals(1, events.size());
        return events.getFirst();
    }

    private List<AuditEvent> recordedAll() {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, atLeastOnce()).recordEvent(event.capture());
        return event.getAllValues();
    }
}
