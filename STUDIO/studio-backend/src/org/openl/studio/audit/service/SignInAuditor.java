package org.openl.studio.audit.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.security.pat.model.PatAuthenticationToken;
import org.openl.studio.session.ClientSessionListener;
import org.openl.studio.session.ClientSessions;
import org.openl.studio.session.CredentialClient;

/**
 * Records sign-ins, failed sign-ins, sign-outs and the starts and ends of sessions in the audit log.
 *
 * <p>A sign-in is recorded when it opens a session: through the login form, SAML or OAuth2. A request that carries
 * its own credentials — Basic authentication, a bearer token, a personal access token — signs in with every request
 * and opens no HTTP session, but OpenL Studio keeps a client session for its credentials ({@link ClientSessions}).
 * Its start is recorded as a started session: at the first request of the credentials, and at the first one after the
 * client session ended. Refused credentials are recorded every time.
 *
 * <p>A sign-out is recorded when a user signs out, or the identity provider signs the user out. A session — of a
 * browser or of credentials — that ends without a sign-out is recorded as ended: after it was left idle for longer
 * than it may stay, marked as timed out, and when a restart of OpenL Studio ends it, marked as restarted. So the
 * started and ended sessions of credentials tell how often OpenL Studio set up the state of a client anew.
 *
 * <p>A failed sign-in names the user only when a user name was typed. A token is never recorded: some of them
 * are read as the name of their holder. A personal access token is named by its public id instead.
 *
 * <p>A problem with the audit log never fails a sign-in: it is logged and the sign-in goes on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignInAuditor implements HandlerInterceptor, ClientSessionListener {

    static final String REASON = "reason";
    static final String TOKEN = "token";
    /** The reason of the end of a session that was left idle for longer than it may stay. */
    static final String TIMED_OUT = "timedOut";
    /** The reason of the end of a session a restart of OpenL Studio ended. */
    static final String RESTARTED = "restarted";
    /** The session attribute that tells why the session is ended. */
    private static final String END_REASON = SignInAuditor.class.getName() + ".endReason";

    private final AuditService auditService;
    private final ClientSessions clientSessions;

    @EventListener
    public void onSignIn(InteractiveAuthenticationSuccessEvent event) {
        var authentication = event.getAuthentication();
        recordQuietly(AuditEvent.builder()
                .action(AuditAction.SIGN_IN)
                .actor(authentication.getName())
                .authentication(AuthenticationMethods.of(authentication))
                .build());
    }

    @EventListener
    public void onSignInFailure(AbstractAuthenticationFailureEvent event) {
        var authentication = event.getAuthentication();
        var details = new LinkedHashMap<String, Object>();
        details.put(REASON, reasonOf(event.getException()));
        if (authentication instanceof PatAuthenticationToken token && token.getPublicId() != null) {
            details.put(TOKEN, token.getPublicId());
        }
        recordQuietly(AuditEvent.builder()
                .action(AuditAction.SIGN_IN)
                .outcome(AuditOutcome.FAILURE)
                .actor(typedUserName(authentication))
                .authentication(AuthenticationMethods.of(authentication))
                .details(details)
                .build());
    }

    @EventListener
    public void onSignOut(LogoutSuccessEvent event) {
        var authentication = event.getAuthentication();
        recordQuietly(AuditEvent.builder()
                .action(AuditAction.SIGN_OUT)
                .actor(authentication.getName())
                .authentication(AuthenticationMethods.of(authentication))
                .build());
    }

    /**
     * Records the end of a session that was left idle for longer than it may stay, or that a restart of OpenL Studio
     * ended.
     *
     * <p>A session that ends otherwise, by a sign-out or a stop of the application, was used just before, and
     * records nothing here.
     */
    @EventListener
    public void onSessionEnd(HttpSessionDestroyedEvent event) {
        var reason = endReasonOf(event.getSession(), System.currentTimeMillis());
        if (reason == null) {
            return;
        }
        event.getSecurityContexts().stream()
                .map(SecurityContext::getAuthentication)
                .filter(Objects::nonNull)
                .forEach(authentication -> recordQuietly(AuditEvent.builder()
                        .action(AuditAction.SESSION_ENDED)
                        .actor(authentication.getName())
                        .authentication(AuthenticationMethods.ofSession(authentication))
                        .details(Map.of(REASON, reason))
                        .build()));
    }

    /**
     * Marks a session a restart of OpenL Studio is about to end, so that its end is recorded.
     */
    public static void endedByRestart(HttpSession session) {
        session.setAttribute(END_REASON, RESTARTED);
    }

    /**
     * Why a session ended, when it ended without a sign-out: it timed out, or a restart ended it.
     */
    private static @Nullable String endReasonOf(HttpSession session, long now) {
        if (timedOut(session, now)) {
            return TIMED_OUT;
        }
        try {
            return session.getAttribute(END_REASON) instanceof String reason ? reason : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Whether a session was left idle for at least the time it may stay idle. A session that never times out, or
     * that tells nothing more, did not.
     */
    static boolean timedOut(HttpSession session, long now) {
        try {
            var idleSeconds = session.getMaxInactiveInterval();
            return idleSeconds > 0
                    && now - session.getLastAccessedTime() >= Duration.ofSeconds(idleSeconds).toMillis();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Opens the client of a request that carries its own credentials, so that the start of the client is heard even
     * when the request uses nothing kept for it. A browser calling with its session opens none. Nothing is opened for
     * the log while it is off.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (auditService.isEnabled()) {
            clientSessions.open();
        }
        return true;
    }

    /** Records the start of the session of a client of credentials. */
    @Override
    public void started(CredentialClient client) {
        recordCredentials(AuditAction.SESSION_STARTED, client, null);
    }

    /** Records the end of the session of a client of credentials that was idle. */
    @Override
    public void timedOut(CredentialClient client) {
        recordCredentials(AuditAction.SESSION_ENDED, client, TIMED_OUT);
    }

    /** Records the ends of the sessions of the clients of credentials a restart of OpenL Studio ended. */
    public void restarted(List<CredentialClient> clients) {
        clients.forEach(client -> recordCredentials(AuditAction.SESSION_ENDED, client, RESTARTED));
    }

    private void recordCredentials(AuditAction action, CredentialClient client, @Nullable String reason) {
        if (!auditService.isEnabled()) {
            return;
        }
        var authentication = client.authentication();
        var details = new LinkedHashMap<String, @Nullable Object>();
        details.put(REASON, reason);
        if (authentication instanceof PatAuthenticationToken token) {
            details.put(TOKEN, token.getPublicId());
        }
        recordQuietly(AuditEvent.builder()
                .action(action)
                .actor(authentication.getName())
                .authentication(AuthenticationMethods.ofCredentials(authentication))
                .details(details)
                .build());
    }

    private void recordQuietly(AuditEvent event) {
        try {
            auditService.recordEvent(event);
        } catch (RuntimeException e) {
            log.error("Failed to record the {} event of '{}' in the audit log.", event.action(), event.actor(), e);
        }
    }

    /**
     * The name the user typed to sign in, or {@code null} when the credentials hold no typed name.
     */
    private static @Nullable String typedUserName(Authentication authentication) {
        if (authentication instanceof PatAuthenticationToken
                || !(authentication instanceof UsernamePasswordAuthenticationToken)) {
            return null;
        }
        var name = authentication.getName();
        return name == null || name.isBlank() ? null : name;
    }

    /**
     * Why the sign-in was refused, without the details an attacker could learn from.
     */
    static String reasonOf(AuthenticationException exception) {
        return switch (exception) {
            case BadCredentialsException ignored -> "badCredentials";
            case UsernameNotFoundException ignored -> "badCredentials";
            case LockedException ignored -> "locked";
            case DisabledException ignored -> "disabled";
            case AccountExpiredException ignored -> "expired";
            case CredentialsExpiredException ignored -> "expired";
            case OAuth2AuthenticationException ignored -> "invalidToken";
            default -> "error";
        };
    }
}
