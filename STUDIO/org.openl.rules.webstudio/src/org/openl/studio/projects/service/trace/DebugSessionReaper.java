package org.openl.studio.projects.service.trace;

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
 */
@Slf4j
@Component
public class DebugSessionReaper {

    private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final long SWEEP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final Set<DebugSession> sessions = ConcurrentHashMap.newKeySet();
    private final long idleTimeoutMillis;
    private final long sweepIntervalMillis;
    private @Nullable ScheduledExecutorService scheduler;

    public DebugSessionReaper() {
        this(IDLE_TIMEOUT_MILLIS, SWEEP_INTERVAL_MILLIS);
    }

    DebugSessionReaper(long idleTimeoutMillis, long sweepIntervalMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.sweepIntervalMillis = sweepIntervalMillis;
    }

    @PostConstruct
    void start() {
        var executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("debug-session-reaper").factory());
        executor.scheduleWithFixedDelay(this::sweepQuietly, sweepIntervalMillis, sweepIntervalMillis,
                TimeUnit.MILLISECONDS);
        scheduler = executor;
    }

    /** Start tracking a session so it can be reaped once idle. */
    public void register(DebugSession session) {
        sessions.add(session);
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
                .forEach(this::reap);
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

    private void reap(DebugSession session) {
        sessions.remove(session);
        try {
            log.warn("Reaping idle debug session (table '{}'): no access for over {} ms",
                    session.getTableId(), idleTimeoutMillis);
            session.terminate();
        } catch (RuntimeException e) {
            log.warn("Failed to reap idle debug session", e);
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        List.copyOf(sessions).forEach(this::reap);
    }
}
