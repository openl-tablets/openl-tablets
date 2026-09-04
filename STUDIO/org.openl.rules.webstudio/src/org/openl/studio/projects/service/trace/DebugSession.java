package org.openl.studio.projects.service.trace;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.trace.TraceDebugMapper;

/**
 * One interactive debug session: a running (or suspended) rule execution and the data needed to
 * inspect it. At most one session is active per user.
 */
@Getter
@RequiredArgsConstructor
public final class DebugSession {

    private static final long TERMINATE_JOIN_MILLIS = 2_000;

    private final ProjectIdModel projectId;
    private final String tableId;
    private final TraceDebugger debugger;
    private final @Nullable ClassLoader classLoader;

    /** Replays the traced execution with a fresh tracer, so the full tree can be exported on demand. */
    private final @Nullable TraceReplay replay;

    /**
     * Identity of this session, carried by every stack view and WebSocket status event. Sessions of the
     * same user and table share one notification topic, so without it a client could not tell a stale
     * session's terminal event (reaped in the background) from its own.
     */
    private final String id;

    /**
     * Serialises controller-side stack inspection against the commands that unpark the worker. A frame's
     * step and condition lists are read while the worker is suspended; without this lock a concurrent
     * step or resume could unpark the worker mid-read and mutate them.
     */
    @Getter(AccessLevel.NONE)
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Lazily-built mapper shared for the whole session. Building it (a Jackson object mapper plus a JSON
     * schema generator) is expensive, and the project's class graph is stable while the session runs, so
     * it is built once on first inspection instead of per request.
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<@Nullable TraceDebugMapper> mapperCache = new AtomicReference<>();

    /** Wall-clock of the last controller access; drives idle reaping. */
    private volatile long lastAccessMillis = System.currentTimeMillis();

    /** Record a controller access so the idle reaper does not reclaim an active session. */
    public void touch() {
        lastAccessMillis = System.currentTimeMillis();
    }

    /** Run a stack inspection under the session lock so it cannot interleave with a step or resume. */
    public <T> T inLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /** Run a command under the session lock so it cannot interleave with a concurrent inspection. */
    public void inLock(Runnable action) {
        inLock(() -> {
            action.run();
            return null;
        });
    }

    /** Return the session mapper, building it once via {@code factory} on first use. */
    public TraceDebugMapper mapper(Supplier<TraceDebugMapper> factory) {
        var existing = mapperCache.get();
        // Build off the fast path once set; on a first-use race updateAndGet keeps the first writer's instance.
        return existing != null ? existing
                : mapperCache.updateAndGet(current -> current != null ? current : factory.get());
    }

    /** Cancel the session, releasing the worker thread. Never blocks on the session lock so it preempts. */
    public void terminate() {
        debugger.terminate(TERMINATE_JOIN_MILLIS);
    }
}
