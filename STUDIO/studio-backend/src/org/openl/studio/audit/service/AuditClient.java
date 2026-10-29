package org.openl.studio.audit.service;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.audit.model.AuditAuthentication;

/**
 * The client an event is recorded for: the address its request comes from and the way its user signed in.
 *
 * <p>An event is recorded for the client of the current request. A task that a request hands to a thread of its own,
 * such as a merge, records its events for the client of that request: the thread pools of OpenL Studio carry the
 * client to the task, as they carry the user.
 *
 * <p>Outside a request and the tasks it started there is no client address, and a user name and a password tell
 * nothing of the way the user signed in. An action OpenL Studio does itself has no client either, even when a request
 * happened to set it off.
 *
 * @param address        the address of the client, or {@code null} outside a request
 * @param authentication the way the signed-in user signed in, or {@code null} when nothing tells it
 */
public record AuditClient(@Nullable String address, @Nullable AuditAuthentication authentication) {

    private static final ThreadLocal<AuditClient> CARRIED = new ThreadLocal<>();

    /**
     * The client of the current request, or of the request that started the task this thread runs.
     */
    static AuditClient current() {
        return Optional.ofNullable(CARRIED.get()).orElseGet(AuditClient::ofRequest);
    }

    /**
     * A task that records its events for the client of the current request, on whatever thread it runs.
     */
    public static Runnable carry(Runnable task) {
        var client = current();
        return () -> runFor(client, task);
    }

    /**
     * Runs an action OpenL Studio does itself, such as a restart: its events are recorded for no client, even on the
     * thread of a request.
     */
    public static void runAsApplication(Runnable action) {
        runFor(new AuditClient(null, null), action);
    }

    private static void runFor(AuditClient client, Runnable task) {
        var outer = CARRIED.get();
        CARRIED.set(client);
        try {
            task.run();
        } finally {
            if (outer == null) {
                CARRIED.remove();
            } else {
                CARRIED.set(outer);
            }
        }
    }

    private static AuditClient ofRequest() {
        var address = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest().getRemoteAddr()
                : null;
        return new AuditClient(address,
                AuthenticationMethods.of(SecurityContextHolder.getContext().getAuthentication()));
    }
}
