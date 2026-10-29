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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
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

class SignInAuditorTest {

    private static final Duration IDLE = Duration.ofMinutes(30);

    private final AtomicLong clock = new AtomicLong();
    private AuditService auditService;
    private SignInAuditor auditor;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        when(auditService.isEnabled()).thenReturn(true);
        auditor = new SignInAuditor(auditService, IDLE, clock::get);
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
    void recordsTheFirstRequestOfCredentialsAsASignIn() {
        auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());
        auditor.preHandle(basicRequest("jsmith"), new MockHttpServletResponse(), new Object());

        clock.addAndGet(Duration.ofMinutes(29).toNanos());
        auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());

        var events = recordedAll();
        assertEquals(List.of("jdoe", "jsmith"), events.stream().map(AuditEvent::actor).toList(),
                "A request soon after the last one is the same use of the credentials");
        assertEquals(AuditAction.SIGN_IN, events.getFirst().action());
        assertEquals(AuditOutcome.SUCCESS, events.getFirst().outcome());
        assertEquals(AuditAuthentication.BASIC, events.getFirst().authentication());
    }

    @Test
    void recordsASignInAgainOnceTheCredentialsWereIdle() {
        auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());
        auditor.preHandle(basicRequest("jsmith"), new MockHttpServletResponse(), new Object());
        clock.addAndGet(IDLE.toNanos());

        auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());
        auditor.preHandle(basicRequest("jsmith"), new MockHttpServletResponse(), new Object());

        assertEquals(List.of("jdoe", "jsmith", "jdoe", "jsmith"),
                recordedAll().stream().map(AuditEvent::actor).toList());
    }

    @Test
    void takesCredentialsForIdleAfterTheIdleTimeOfASession() {
        var servletContext = new MockServletContext();
        servletContext.setSessionTimeout(30);
        var configured = new SignInAuditor(auditService, servletContext);

        configured.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());
        configured.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());

        assertEquals("jdoe", recorded().actor(), "The second request continues the use the first one started");
    }

    @Test
    void namesThePersonalAccessTokenASignInUsed() {
        var request = requestTo("/rest/projects");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Token openl_pat_abc123DEF4567890.secret");
        SecurityContextHolder.getContext().setAuthentication(
                new PatAuthenticationToken("jdoe", null, List.of(), "abc123DEF4567890"));

        auditor.preHandle(request, new MockHttpServletResponse(), new Object());

        var event = recorded();
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.TOKEN, event.authentication());
        assertEquals(Map.of(SignInAuditor.TOKEN, "abc123DEF4567890"), event.details());
    }

    @Test
    void recordsNoSignInOfABrowserCallingWithItsSession() {
        var request = basicRequest("jdoe");
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(signedIn("jdoe")));
        auditor.preHandle(request, new MockHttpServletResponse(), new Object());
        auditor.preHandle(requestTo("/rest/projects"), new MockHttpServletResponse(), new Object());

        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void recordsNoSignInWhileTheLogIsOff() {
        when(auditService.isEnabled()).thenReturn(false);

        auditor.preHandle(basicRequest("jdoe"), new MockHttpServletResponse(), new Object());

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
