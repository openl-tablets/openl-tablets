package org.openl.studio.projects.service.trace;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Reclaims abandoned debug sessions.
 *
 * <p>A suspended session keeps a worker thread parked on its live continuation, holding the frozen
 * object graph. The client-scoped registry releases it when a new session starts, on cancel, or when
 * the client ends. None of those fire when the user simply walks away from a breakpoint,
 * closes the tab, or the launch popup was blocked.
 *
 * <p>This singleton is the safety net: it periodically terminates sessions that have not been accessed
 * within the idle timeout, bounding how long an orphaned worker can hold memory. It also doubles as a
 * backstop for a runaway rule that never reaches a step point.
 *
 * <p>It further caps the total number of live sessions: because the one-per-user limit only holds within a
 * single browser session, one user opening several sessions could otherwise pile up workers and heap. When
 * a new session pushes the count over the limit, the least-recently-accessed session is reclaimed first.
 *
 * <p>The executed trees the business view keeps after a run ({@link TraceTreeSnapshot}) are released once idle for
 * the same timeout. Together they hold at most as many nodes as one run may record: when a new tree pushes the total
 * over that budget, the least-recently-read trees are released first.
 */
@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DebugSessionReaper {

    private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final long SWEEP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final int MAX_ACTIVE_SESSIONS = 16;
    private static final long MAX_KEPT_TREE_NODES = DebugHookImpl.MAX_TREE_NODES;

    private final Set<DebugSession> sessions = ConcurrentHashMap.newKeySet();
    private final Set<TraceTreeSnapshot> trees = ConcurrentHashMap.newKeySet();
    private final long idleTimeoutMillis;
    private final long sweepIntervalMillis;
    private final int maxActiveSessions;
    private final long maxKeptTreeNodes;
    private @Nullable ScheduledExecutorService scheduler;

    public DebugSessionReaper() {
        this(IDLE_TIMEOUT_MILLIS, SWEEP_INTERVAL_MILLIS, MAX_ACTIVE_SESSIONS, MAX_KEPT_TREE_NODES);
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

    /** Start tracking a kept tree so it is released once idle, releasing the oldest if the node budget is exceeded. */
    public void register(TraceTreeSnapshot tree) {
        trees.add(tree);
        enforceTreeBudget();
    }

    /**
     * Bound the number of concurrently tracked sessions across the whole application.
     *
     * <p>The client-scoped registry keeps only one active session per client, but a single
     * user can open several (multiple browsers, API clients) and each holds a worker thread and its frozen
     * object graph. This global limit reclaims the least-recently-accessed sessions once the total exceeds
     * the cap, so an actively used session survives while stale or abandoned ones are dropped first.
     */
    private void enforceCapacity() {
        releaseOldest(sessions, DebugSession::getLastAccessMillis, () -> sessions.size() > maxActiveSessions,
                session -> reap(session, "active session limit " + maxActiveSessions + " exceeded"));
    }

    /**
     * Bound the nodes the kept trees hold across the whole application. A tree is as large as its run, up to the
     * millions of nodes one run may record, so the trees are limited by their size rather than by their number.
     * The least-recently-read trees are released first, so the tree a user is browsing survives.
     */
    private void enforceTreeBudget() {
        releaseOldest(trees, TraceTreeSnapshot::getLastAccessMillis,
                () -> trees.stream().mapToLong(TraceTreeSnapshot::getNodeCount).sum() > maxKeptTreeNodes, tree -> {
                    log.warn("Releasing the kept trace tree of {} nodes: kept trees exceed {} nodes",
                            tree.getNodeCount(), maxKeptTreeNodes);
                    release(tree);
                });
    }

    /** Release the least-recently-accessed of the tracked while {@code over} holds; {@code release} untracks it. */
    private static <T> void releaseOldest(Set<T> tracked, ToLongFunction<T> lastAccess, BooleanSupplier over,
                                          Consumer<T> release) {
        while (over.getAsBoolean()) {
            var oldest = tracked.stream().min(Comparator.comparingLong(lastAccess));
            if (oldest.isEmpty()) {
                return;
            }
            release.accept(oldest.get());
        }
    }

    /** Release every one of the tracked idle beyond the timeout. */
    private <T> void releaseIdle(Set<T> tracked, ToLongFunction<T> lastAccess, Consumer<T> release) {
        var now = System.currentTimeMillis();
        tracked.stream()
                .filter(item -> now - lastAccess.applyAsLong(item) >= idleTimeoutMillis)
                .toList()
                .forEach(release);
    }

    /** Stop tracking a session that has already been released. */
    public void unregister(DebugSession session) {
        sessions.remove(session);
    }

    /** Stop tracking a kept tree that has already been released. */
    public void unregister(TraceTreeSnapshot tree) {
        trees.remove(tree);
    }

    /** Terminate and drop every session, and release every kept tree, idle beyond the timeout. */
    void sweep() {
        releaseIdle(sessions, DebugSession::getLastAccessMillis,
                session -> reap(session, "idle for over " + idleTimeoutMillis + " ms"));
        releaseIdle(trees, TraceTreeSnapshot::getLastAccessMillis, this::release);
    }

    int trackedCount() {
        return sessions.size();
    }

    int trackedTreeCount() {
        return trees.size();
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

    private void release(TraceTreeSnapshot tree) {
        if (trees.remove(tree)) {
            tree.release();
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        List.copyOf(sessions).forEach(session -> reap(session, "reaper shutdown"));
        List.copyOf(trees).forEach(this::release);
    }
}
