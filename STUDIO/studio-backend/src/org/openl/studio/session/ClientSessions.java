package org.openl.studio.session;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.authentication.logout.LogoutTokenClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;

import org.openl.studio.security.pat.model.PatAuthenticationToken;

/**
 * Keeps what OpenL Studio holds for one client between its requests.
 *
 * <p>A browser signs in once and sends its session cookie with every request, so what it is kept for lives
 * in its HTTP session. A request that carries its own credentials — a bearer token, a personal access token,
 * Basic authentication — opens no session. What it is kept for lives here instead, under the credential it
 * came with: the next request with the same credential finds the project it compiled and the tests it ran.
 *
 * <p>A credential is told apart by what it stands for, never by the token itself:
 * <ul>
 *     <li>a personal access token, by its public id;</li>
 *     <li>a token of the identity provider, by the user and the provider's sign-in ({@code sid}), which stays
 *     the same while the access token is refreshed;</li>
 *     <li>anything else, by the user.</li>
 * </ul>
 *
 * <p>A request that brings credentials together with the session cookie of a browser the same user signed in
 * to belongs to that browser: it is the browser calling, the API documentation page for one.
 *
 * <p>What a credential is kept for ends once its requests have stopped for as long as an HTTP session would
 * last — never while one of them is running, and never at all when sessions never expire — and with the
 * application. Its beans are then destroyed, the latest created first, as they are when a session ends.
 */
@Slf4j
public class ClientSessions implements Scope, InitializingBean, DisposableBean {

    /** The name the scope is registered under. */
    public static final String NAME = "clientSession";

    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    /** The request attribute holding the client a request was found to belong to. */
    private static final String CLIENT = ClientSessions.class.getName() + ".client";

    /** What the request attribute holds for a request that belongs to a browser session. */
    private static final Object BROWSER = new Object();

    private final Scope httpSessions = new SessionScope();
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final long idleTimeoutNanos;
    private final LongSupplier nanoClock;
    private @Nullable ScheduledExecutorService sweeper;

    /**
     * Keeps a credential's state for the given idle time; a time of zero or less keeps it for good, as a session
     * timeout of zero or less does.
     */
    public ClientSessions(Duration idleTimeout) {
        this(idleTimeout, System::nanoTime);
    }

    /** Tells the time by the given clock of nanoseconds; nothing is swept until the bean is started. */
    ClientSessions(Duration idleTimeout, LongSupplier nanoClock) {
        this.idleTimeoutNanos = idleTimeout.toNanos();
        this.nanoClock = nanoClock;
    }

    /**
     * Starts sweeping away the state of idle credentials, once a minute, unless it is kept for good.
     */
    @Override
    public void afterPropertiesSet() {
        if (idleTimeoutNanos <= 0) {
            return;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("client-sessions-sweeper").factory());
        executor.scheduleWithFixedDelay(this::sweepQuietly, SWEEP_INTERVAL.toMillis(), SWEEP_INTERVAL.toMillis(),
                TimeUnit.MILLISECONDS);
        sweeper = executor;
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        var client = currentClient();
        return client == null ? httpSessions.get(name, objectFactory) : client.get(name, objectFactory);
    }

    @Override
    public @Nullable Object remove(String name) {
        var client = currentClient();
        return client == null ? httpSessions.remove(name) : client.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        var client = currentClient();
        if (client == null) {
            httpSessions.registerDestructionCallback(name, callback);
        } else {
            client.registerDestructionCallback(name, callback);
        }
    }

    @Override
    public @Nullable Object resolveContextualObject(String key) {
        return currentClient() == null ? httpSessions.resolveContextualObject(key) : null;
    }

    @Override
    public @Nullable String getConversationId() {
        var client = currentClient();
        return client == null ? httpSessions.getConversationId() : client.credential;
    }

    /**
     * Ends what every credential is kept for, as the application stops or reloads.
     */
    @Override
    public void destroy() {
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
        for (var credential : clients.keySet()) {
            var client = clients.remove(credential);
            if (client != null) {
                client.destroy();
            }
        }
    }

    /**
     * Ends what the credentials whose requests have stopped for the idle time are kept for.
     */
    void sweep() {
        if (idleTimeoutNanos <= 0) {
            return;
        }
        var now = nanoClock.getAsLong();
        for (var credential : clients.keySet()) {
            var ended = new AtomicReference<Client>();
            // Taken out only while still idle: a request that has just arrived keeps it.
            clients.computeIfPresent(credential, (key, client) -> {
                if (!client.isIdle(now, idleTimeoutNanos)) {
                    return client;
                }
                ended.set(client);
                return null;
            });
            var idle = ended.get();
            if (idle != null) {
                log.debug("The state kept for a client has been idle for {} and is released.",
                        Duration.ofNanos(idleTimeoutNanos));
                idle.destroy();
            }
        }
    }

    /** How many credentials something is kept for now. */
    int size() {
        return clients.size();
    }

    private void sweepQuietly() {
        try {
            sweep();
        } catch (RuntimeException e) {
            log.error("Failed to release the state of idle clients.", e);
        }
    }

    /**
     * The client of the current request, or null when it belongs to a browser session.
     *
     * <p>Found once per request and kept on it: the request is counted among the ones its client is running until
     * it ends, and every bean it asks for later is found without looking again.
     */
    private @Nullable Client currentClient() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        var found = attributes.getAttribute(CLIENT, RequestAttributes.SCOPE_REQUEST);
        if (found != null) {
            return found instanceof Client client ? client : null;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var credential = credentialOf(attributes.getRequest(), authentication);
        if (credential == null) {
            // Settled only once the request is signed in: before that, it may still turn out to carry credentials.
            if (authentication != null && authentication.isAuthenticated()) {
                attributes.setAttribute(CLIENT, BROWSER, RequestAttributes.SCOPE_REQUEST);
            }
            return null;
        }
        var client = clients.compute(credential, (key, known) -> {
            if (known == null) {
                log.debug("Keeps state for the new client '{}'.", key);
            }
            var entered = known == null ? new Client(key) : known;
            entered.running.incrementAndGet();
            return entered;
        });
        attributes.setAttribute(CLIENT, client, RequestAttributes.SCOPE_REQUEST);
        attributes.registerDestructionCallback(CLIENT,
                () -> client.leave(nanoClock.getAsLong()),
                RequestAttributes.SCOPE_REQUEST);
        return client;
    }

    /**
     * What the credentials of a request stand for, or null when the request carries none of its own.
     *
     * <p>Only a request with the {@code Authorization} header carries its own credentials: a browser proves who
     * it is with its session cookie alone. A request that also belongs to a browser session of the same user is
     * that browser calling, and keeps to its session.
     */
    static @Nullable String credentialOf(HttpServletRequest request, @Nullable Authentication authentication) {
        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null || authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }
        var user = authentication.getName();
        if (user.equals(signedInToSession(request))) {
            return null;
        }
        return switch (authentication) {
            case PatAuthenticationToken pat -> user + "|pat:" + pat.getPublicId();
            case JwtAuthenticationToken jwt -> signedIn(user, jwt.getToken().getClaimAsString(LogoutTokenClaimNames.SID));
            case BearerTokenAuthentication opaque ->
                    signedIn(user, opaque.getTokenAttributes().get(LogoutTokenClaimNames.SID));
            default -> user;
        };
    }

    private static String signedIn(String user, @Nullable Object signIn) {
        return signIn == null ? user : user + "|sid:" + signIn;
    }

    /** Who signed in to the browser session the request belongs to, or null when it belongs to none. */
    private static @Nullable String signedInToSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null
                && session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                instanceof SecurityContext context
                && context.getAuthentication() instanceof Authentication signedIn) {
            return signedIn.getName();
        }
        return null;
    }

    /** The beans kept for one credential. */
    private final class Client {

        private final String credential;
        private final Map<String, Object> beans = new HashMap<>();
        private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();
        /** How many requests of the client are running now; a client with one running is never idle. */
        private final AtomicInteger running = new AtomicInteger();
        private volatile long lastAccess = nanoClock.getAsLong();

        Client(String credential) {
            this.credential = credential;
        }

        /** A request of the client has ended: the client was last used now. */
        void leave(long now) {
            lastAccess = now;
            running.decrementAndGet();
        }

        boolean isIdle(long now, long idleTimeout) {
            return running.get() == 0 && now - lastAccess >= idleTimeout;
        }

        // computeIfAbsent cannot be used: creating a bean reads this map again, see the comment below.
        @SuppressWarnings("java:S3824")
        synchronized Object get(String name, ObjectFactory<?> objectFactory) {
            // Not computeIfAbsent: creating a bean asks this client for the beans it is made from, and a map
            // changed from inside its own computeIfAbsent throws ConcurrentModificationException.
            var bean = beans.get(name);
            if (bean == null) {
                bean = objectFactory.getObject();
                beans.put(name, bean);
            }
            return bean;
        }

        synchronized @Nullable Object remove(String name) {
            destructionCallbacks.remove(name);
            return beans.remove(name);
        }

        synchronized void registerDestructionCallback(String name, Runnable callback) {
            destructionCallbacks.put(name, callback);
        }

        synchronized void destroy() {
            // The latest created first: a bean ends before the beans it was created from, as Spring ends them.
            // Walked over a copy, so a bean that removes another one while it ends does not break the walk.
            for (var entry : new ArrayList<>(destructionCallbacks.entrySet()).reversed()) {
                try {
                    entry.getValue().run();
                } catch (RuntimeException e) {
                    log.error("Failed to destroy the bean '{}' kept for a client.", entry.getKey(), e);
                }
            }
            destructionCallbacks.clear();
            beans.clear();
        }
    }
}
