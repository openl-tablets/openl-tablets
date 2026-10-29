package org.openl.studio.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.security.pat.model.PatAuthenticationToken;

class ClientSessionsTest {

    private static final Duration IDLE = Duration.ofMinutes(30);

    private final AtomicLong now = new AtomicLong(1_000_000);
    /** What the listener heard, as "started admin" or "timedOut admin". */
    private final List<String> heard = new ArrayList<>();
    private final ClientSessionListener listener = new ClientSessionListener() {
        @Override
        public void started(CredentialClient client) {
            heard.add("started " + client.key());
        }

        @Override
        public void timedOut(CredentialClient client) {
            heard.add("timedOut " + client.key());
        }
    };
    private final ClientSessions scope = new ClientSessions(IDLE, false, List.of(listener), now::get);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aBrowserKeepsItsBeansInItsSession() {
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        signIn(user("admin"));

        var bean = scope.get("studio", Object::new);

        assertSame(bean, session.getAttribute("studio"));
        assertSame(bean, scope.get("studio", Object::new));
        assertEquals(session.getId(), scope.getConversationId());
        assertSame(session, request.getSession(false));
        assertEquals(0, scope.size());
    }

    @Test
    void inSingleUserModeEveryRequestKeepsItsBeansForTheOneUser() {
        var single = new ClientSessions(IDLE, true, List.of(), now::get);
        signIn(new AnonymousAuthenticationToken("key", "DEFAULT", AuthorityUtils.createAuthorityList("ADMIN")));
        var browser = new MockHttpServletRequest();
        browser.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(browser));
        var bean = single.get("studio", Object::new);
        complete();

        var cookieless = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(cookieless));

        assertSame(bean, single.get("studio", Object::new));
        assertNull(browser.getSession(false).getAttribute("studio"));
        assertNull(cookieless.getSession(false));
        assertEquals("DEFAULT", single.getConversationId());
        assertEquals(1, single.size());
    }

    @Test
    void inSingleUserModeARequestNotSignedInYetKeepsToItsSession() {
        var single = new ClientSessions(IDLE, true, List.of(), now::get);
        var request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var bean = single.get("studio", Object::new);

        assertSame(bean, request.getSession(false).getAttribute("studio"));
        assertEquals(0, single.size());
    }

    @Test
    void aRequestWithItsOwnCredentialsKeepsItsBeansForThemWithoutASession() {
        signIn(user("admin"));
        var first = stateless();
        var bean = scope.get("studio", Object::new);
        complete();

        var second = stateless();

        assertSame(bean, scope.get("studio", Object::new));
        assertNull(first.getSession(false));
        assertNull(second.getSession(false));
        assertEquals("admin", scope.getConversationId());
        assertNull(scope.resolveContextualObject("session"));
    }

    @Test
    void anotherUserHasBeansOfTheirOwn() {
        signIn(user("admin"));
        stateless();
        var admin = scope.get("studio", Object::new);

        signIn(user("guest"));
        stateless();

        assertNotSame(admin, scope.get("studio", Object::new));
        assertEquals(2, scope.size());
    }

    @Test
    void aSignInAtTheIdentityProviderIsOneClientWhateverAccessTokenItSends() {
        var request = withAuthorization("Bearer x");

        var first = ClientSessions.credentialOf(request, jwt("first-token", "sign-in-1"));
        var refreshed = ClientSessions.credentialOf(request, jwt("refreshed-token", "sign-in-1"));
        var another = ClientSessions.credentialOf(request, jwt("third-token", "sign-in-2"));

        assertEquals(first, refreshed);
        assertNotEquals(first, another);
        assertEquals("admin", ClientSessions.credentialOf(request, jwt("service-token", null)));
    }

    @Test
    void anOpaqueTokenIsToldApartByItsSignInToo() {
        var request = withAuthorization("Bearer x");

        var signedIn = ClientSessions.credentialOf(request, opaque(Map.of("sub", "admin", "sid", "sign-in-1")));
        var service = ClientSessions.credentialOf(request, opaque(Map.of("sub", "admin")));

        assertEquals("admin|sid:sign-in-1", signedIn);
        assertEquals("admin", service);
    }

    @Test
    void aPersonalAccessTokenIsOneClientOfItsOwn() {
        var request = withAuthorization("Token openl_pat_x");

        var first = ClientSessions.credentialOf(request, pat("abcdefghijklmnop"));
        var second = ClientSessions.credentialOf(request, pat("qrstuvwxyzabcdef"));

        assertNotNull(first);
        assertNotEquals(first, second);
        assertNotEquals("admin", first);
    }

    @Test
    void aRequestWithoutItsOwnCredentialsOrUnauthenticatedBelongsToItsSession() {
        var withHeader = withAuthorization("Basic x");

        assertNull(ClientSessions.credentialOf(new MockHttpServletRequest(), user("admin")));
        assertNull(ClientSessions.credentialOf(withHeader, null));
        assertNull(ClientSessions.credentialOf(withHeader,
                UsernamePasswordAuthenticationToken.unauthenticated("admin", "admin")));
    }

    @Test
    void aBrowserCallingWithCredentialsOfItsOwnUserKeepsToItsSession() {
        var session = new MockHttpSession();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(user("admin"));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var request = withAuthorization("Token openl_pat_x");
        request.setSession(session);

        assertNull(ClientSessions.credentialOf(request, pat("abcdefghijklmnop")));
        // Credentials of another user never reach the session a browser signed in to.
        assertEquals("guest", ClientSessions.credentialOf(request, user("guest")));
    }

    @Test
    void aRequestWithoutCredentialsEntersTheClientItNamesByItsKey() {
        signIn(user("admin"));
        stateless();
        var bean = scope.get("studio", Object::new);
        var key = scope.currentKey();
        complete();
        SecurityContextHolder.clearContext();

        var link = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(link));
        var leave = scope.enter(link, key);

        assertEquals("admin", key);
        assertNotNull(leave);
        assertSame(bean, scope.get("studio", Object::new));
        assertNull(link.getSession(false));

        // Not idle while the request runs, idle once it has left
        now.addAndGet(IDLE.toNanos() * 2);
        scope.sweep();
        assertEquals(1, scope.size());
        leave.run();
        now.addAndGet(IDLE.toNanos());
        scope.sweep();
        assertEquals(0, scope.size());
    }

    @Test
    void aKeyNothingIsKeptUnderAnyMoreIsNotEntered() {
        var request = new MockHttpServletRequest();

        assertNull(scope.enter(request, "admin|pat:gone"));
        assertEquals(0, scope.size());
    }

    @Test
    void aBrowserSessionHasNoKey() {
        var request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        signIn(user("admin"));

        assertNull(scope.currentKey());
    }

    @Test
    void theBeansOfAnIdleClientAreDestroyed() {
        var destroyed = keepStudio(scope);
        complete();

        now.addAndGet(IDLE.toNanos() - 1);
        scope.sweep();
        assertEquals(List.of(), destroyed);

        // A request just before the timeout keeps the client for another idle period.
        stateless();
        scope.get("studio", Object::new);
        complete();
        now.addAndGet(IDLE.toNanos() - 1);
        scope.sweep();
        assertEquals(List.of(), destroyed);

        now.addAndGet(1);
        scope.sweep();
        assertEquals(List.of("studio"), destroyed);
        assertEquals(0, scope.size());
    }

    @Test
    void aClientIsNotIdleWhileARequestOfItIsRunning() {
        var destroyed = keepStudio(scope);

        // A request running longer than the idle time keeps what it works with.
        now.addAndGet(IDLE.toNanos() * 2);
        scope.sweep();
        assertEquals(List.of(), destroyed);

        // The idle time starts when the request ends, not when it last asked for a bean.
        complete();
        scope.sweep();
        assertEquals(List.of(), destroyed);
        now.addAndGet(IDLE.toNanos());
        scope.sweep();
        assertEquals(List.of("studio"), destroyed);
    }

    @Test
    void aClientIsKeptForGoodWhenSessionsNeverExpire() {
        var forGood = new ClientSessions(Duration.ZERO, false, List.of(), now::get);
        forGood.afterPropertiesSet();
        var destroyed = keepStudio(forGood);
        complete();

        now.addAndGet(Duration.ofDays(365).toNanos());
        forGood.sweep();

        assertEquals(List.of(), destroyed);
        forGood.destroy();
        assertEquals(List.of("studio"), destroyed);
    }

    @Test
    void theEndOfTheApplicationDestroysTheBeansOfEveryClientTheLatestFirst() {
        var destroyed = keepStudio(scope);
        scope.registerDestructionCallback("broken", () -> {
            throw new IllegalStateException("A bean that fails to end");
        });
        scope.registerDestructionCallback("registry", () -> destroyed.add("registry"));

        scope.destroy();

        // One bean failing to end does not keep the others alive, and a bean ends before the ones it was
        // created from.
        assertEquals(List.of("registry", "studio"), destroyed);
        assertEquals(0, scope.size());
    }

    @Test
    void aRemovedBeanIsNotDestroyedWithItsClient() {
        var destroyed = keepStudio(scope);

        assertNotNull(scope.remove("studio"));
        scope.destroy();

        assertEquals(List.of(), destroyed);
        assertNull(scope.remove("unknown"));
    }

    @Test
    void aStartedScopeStopsSweepingAndEndsItsClientsWithTheApplication() {
        var started = new ClientSessions(IDLE, false, List.of());
        started.afterPropertiesSet();
        var destroyed = keepStudio(started);

        started.destroy();

        assertEquals(List.of("studio"), destroyed);
        assertEquals(0, started.size());
    }

    @Test
    void aClientOfCredentialsIsHeardWhenItStartsAndWhenItIsReleasedAfterIdle() {
        var admin = user("admin");
        signIn(admin);
        stateless();
        scope.open();
        complete();
        stateless();
        scope.open();
        complete();

        assertEquals(List.of("started admin"), heard, "The second request goes on with the client the first started");
        assertEquals(1, scope.size());

        now.addAndGet(IDLE.toNanos());
        scope.sweep();
        assertEquals(0, scope.size());
        stateless();
        scope.open();
        complete();

        assertEquals(List.of("started admin", "timedOut admin", "started admin"), heard);
        assertEquals(List.of(new CredentialClient("admin", admin)), scope.clientsToEnd());
        now.addAndGet(IDLE.toNanos());
        scope.sweep();
        assertEquals(1, scope.size(), "A client listed to end with the application is not released as idle");
    }

    @Test
    void aClientListedToEndWithTheApplicationIsNotReleasedAsIdleToo() throws Exception {
        var sweepUnderWay = new CountDownLatch(1);
        var sweepGoesOn = new CountDownLatch(1);
        var timedOut = new CopyOnWriteArrayList<String>();
        var slow = new ClientSessionListener() {
            @Override
            public void started(CredentialClient client) {
                // Only the releases matter here
            }

            @Override
            public void timedOut(CredentialClient client) {
                timedOut.add(client.key());
                sweepUnderWay.countDown();
                awaitQuietly(sweepGoesOn);
            }
        };
        var sessions = new ClientSessions(IDLE, false, List.of(slow), now::get);
        for (var name : List.of("admin", "jsmith")) {
            signIn(user(name));
            stateless();
            sessions.open();
            complete();
        }
        now.addAndGet(IDLE.toNanos());

        var sweep = Thread.ofVirtual().start(sessions::sweep);
        assertTrue(sweepUnderWay.await(5, TimeUnit.SECONDS));
        var ending = CompletableFuture.supplyAsync(sessions::clientsToEnd);
        assertThrows(TimeoutException.class, () -> ending.get(200, TimeUnit.MILLISECONDS),
                "The clients are listed once the sweep under way is over");
        sweepGoesOn.countDown();
        sweep.join();

        assertEquals(List.of(), ending.get(5, TimeUnit.SECONDS), "The sweep has released every client");
        assertEquals(2, timedOut.size());
    }

    @Test
    void neitherABrowserNorTheEndOfTheApplicationIsHeard() {
        var request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        signIn(user("admin"));
        scope.open();
        assertEquals(List.of(), heard, "A browser is no client of credentials");
        assertEquals(0, scope.size());
        keepStudio(scope);

        scope.destroy();

        assertEquals(List.of("started admin"), heard);
    }

    @Test
    void aListenerThatFailsFailsNoRequest() {
        var failing = new ClientSessionListener() {
            @Override
            public void started(CredentialClient client) {
                throw new IllegalStateException("The audit log is down");
            }

            @Override
            public void timedOut(CredentialClient client) {
                throw new IllegalStateException("The audit log is down");
            }
        };
        var sessions = new ClientSessions(IDLE, false, List.of(failing), now::get);
        keepStudio(sessions);
        complete();
        now.addAndGet(IDLE.toNanos());

        assertDoesNotThrow(sessions::sweep);
        assertEquals(0, sessions.size());
    }

    @Test
    void listenersThatCannotBeHadFailNoRequest() {
        Iterable<ClientSessionListener> closing = () -> {
            throw new IllegalStateException("The context is closing");
        };
        var sessions = new ClientSessions(IDLE, false, closing, now::get);

        assertDoesNotThrow(() -> keepStudio(sessions));
        assertEquals(1, sessions.size());
    }

    @Test
    void inSingleUserModeNoClientIsHeardOrListed() {
        var single = new ClientSessions(IDLE, true, List.of(listener), now::get);
        signIn(user("admin"));
        stateless();
        single.open();
        complete();
        now.addAndGet(IDLE.toNanos());
        single.sweep();
        stateless();
        single.open();

        assertEquals(List.of(), heard);
        assertEquals(List.of(), single.clientsToEnd());
        assertEquals(1, single.size());
    }

    /**
     * Signs a stateless request in as {@code admin}, keeps a bean for it in the given scope, and answers the
     * names of the beans destroyed from then on.
     */
    private List<String> keepStudio(ClientSessions sessions) {
        signIn(user("admin"));
        stateless();
        var destroyed = new ArrayList<String>();
        sessions.get("studio", Object::new);
        sessions.registerDestructionCallback("studio", () -> destroyed.add("studio"));
        return destroyed;
    }

    private static MockHttpServletRequest withAuthorization(String value) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", value);
        return request;
    }

    /** Starts a request that proves who it is with its own credentials, and sends no session cookie. */
    private static MockHttpServletRequest stateless() {
        var request = withAuthorization("Basic YWRtaW46YWRtaW4=");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Ends the current request, as the dispatcher does once it has answered. */
    private static void complete() {
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).requestCompleted();
    }

    private static void signIn(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Authentication user(String name) {
        return UsernamePasswordAuthenticationToken.authenticated(name, null, List.of());
    }

    private static Authentication jwt(String token, String signIn) {
        var jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("admin")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (signIn != null) {
            jwt.claim("sid", signIn);
        }
        return new JwtAuthenticationToken(jwt.build(), List.of(), "admin");
    }

    private static Authentication opaque(Map<String, Object> attributes) {
        var principal = new DefaultOAuth2AuthenticatedPrincipal("admin", attributes, List.of());
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "opaque",
                Instant.now(), Instant.now().plusSeconds(300));
        return new BearerTokenAuthentication(principal, token, List.of());
    }

    private static Authentication pat(String publicId) {
        return new PatAuthenticationToken("admin", null, List.of(), publicId);
    }
}
