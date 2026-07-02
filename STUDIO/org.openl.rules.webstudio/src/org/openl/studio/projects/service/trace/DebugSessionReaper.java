package org.openl.studio.projects.service.trace;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Reclaims abandoned debug sessions.
 *
 * <p>A suspended session keeps a worker thread parked on its live continuation, holding the frozen
 * object graph. The session-scoped registry releases it when a new session starts, on cancel, or when
 * the HTTP session is destroyed. None of those fire when the user simply walks away from a breakpoint,
 * closes the tab, or the launch popup was blocked.
 *
 * <p>This singleton is the safety net: it periodically terminates sessions that have not been accessed
 * within the idle timeout, bounding how long an orphaned worker can hold memory. It also doubles as a
 * backstop for a runaway rule that never reaches a step point.
 *
 * <p>It further caps the total number of live sessions: because the one-per-user limit only holds within a
 * single browser session, one user opening several sessions could otherwise pile up workers and heap. When
 * a new session pushes the count over the limit, the least-recently-accessed session is reclaimed first.
 */
@Slf4j
@Component
public class DebugSessionReaper {

    private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final long SWEEP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final int MAX_ACTIVE_SESSIONS = 16;

    private final Set<DebugSession> sessions = ConcurrentHashMap.newKeySet();
    private final long idleTimeoutMillis;
    private final long sweepIntervalMillis;
    private final int maxActiveSessions;
    private @Nullable ScheduledExecutorService scheduler;

    public DebugSessionReaper() {
        this(IDLE_TIMEOUT_MILLIS, SWEEP_INTERVAL_MILLIS, MAX_ACTIVE_SESSIONS);
    }

    DebugSessionReaper(long idleTimeoutMillis, long sweepIntervalMillis, int maxActiveSessions) {
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.sweepIntervalMillis = sweepIntervalMillis;
        this.maxActiveSessions = maxActiveSessions;
    }

    @PostConstruct
    void start() {
        var executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("debug-session-reaper").factory());
        executor.scheduleWithFixedDelay(this::sweepQuietly, sweepIntervalMillis, sweepIntervalMillis,
                TimeUnit.MILLISECONDS);
        scheduler = executor;
    }

    /** Start tracking a session so it can be reaped once idle, evicting the oldest if the global limit is exceeded. */
    public void register(DebugSession session) {
        sessions.add(session);
        enforceCapacity();
    }

    /**
     * Bound the number of concurrently tracked sessions across the whole application.
     *
     * <p>The per-HTTP-session registry keeps only one active session per browser session, but a single
     * user can open several (multiple browsers, API clients) and each holds a worker thread and its frozen
     * object graph. This global limit reclaims the least-recently-accessed sessions once the total exceeds
     * the cap, so an actively used session survives while stale or abandoned ones are dropped first.
     */
    private void enforceCapacity() {
        while (sessions.size() > maxActiveSessions) {
            var oldest = sessions.stream()
                    .min(Comparator.comparingLong(DebugSession::getLastAccessMillis))
                    .orElse(null);
            if (oldest == null) {
                break;
            }
            reap(oldest, "active session limit " + maxActiveSessions + " exceeded");
        }
    }

    /** Stop tracking a session that has already been released. */
    public void unregister(DebugSession session) {
        sessions.remove(session);
    }

    /** Terminate and drop every session idle beyond the timeout. */
    void sweep() {
        long now = System.currentTimeMillis();
        sessions.stream()
                .filter(session -> now - session.getLastAccessMillis() >= idleTimeoutMillis)
                .toList()
                .forEach(session -> reap(session, "idle for over " + idleTimeoutMillis + " ms"));
    }

    int trackedCount() {
        return sessions.size();
    }

    private void sweepQuietly() {
        try {
            sweep();
        } catch (RuntimeException e) {
            log.warn("Debug session sweep failed", e);
        }
    }

    private void reap(DebugSession session, String reason) {
        if (!sessions.remove(session)) {
            return; // already reclaimed by a concurrent sweep or capacity check
        }
        try {
            log.warn("Reaping debug session (table '{}'): {}", session.getTableId(), reason);
            session.terminate();
        } catch (RuntimeException e) {
            log.warn("Failed to reap debug session", e);
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        List.copyOf(sessions).forEach(session -> reap(session, "reaper shutdown"));
    }
}
