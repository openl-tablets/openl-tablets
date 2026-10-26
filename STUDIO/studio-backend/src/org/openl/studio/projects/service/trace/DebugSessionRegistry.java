package org.openl.studio.projects.service.trace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.rules.workspace.ProjectId;
import org.openl.studio.session.ClientSessionScope;

/**
 * Client-scoped holder for the user's active debug session and breakpoints.
 *
 * <p>Holds at most one debug session; starting a new one terminates the previous. Breakpoints persist
 * across runs so the user can set them before starting and they apply to the next run. Everything is
 * released when a new session starts, on explicit cancel, when the workspace is reset, or when the client
 * ends — its HTTP session, or the idle credentials of a stateless client.
 *
 * <p>It also keeps the executed trees of the business-view runs ({@link TraceTreeSnapshot}), one for each run, so
 * every open trace window loads the deep branches of its own run. A tree outlives its session: the clicks that re-run
 * the calculation start and cancel sessions of their own. A tree is released by its window (a new run, the window
 * closing), when the workspace is reset, when the client ends, or once idle; ending a session leaves them. Any trace
 * request of the client counts as an access to its trees.
 */
@Slf4j
@Component
@ClientSessionScope
@RequiredArgsConstructor
public class DebugSessionRegistry {

    private final AtomicReference<DebugSession> ref = new AtomicReference<>();
    /** The kept trees by run. Guarded by this registry, so a tree is never kept and released at once. */
    private final Map<String, TraceTreeSnapshot> trees = new HashMap<>();
    private final Set<String> breakpoints = ConcurrentHashMap.newKeySet();
    private final Set<String> watches = ConcurrentHashMap.newKeySet();
    private final DebugSessionReaper reaper;
    /** Input JSON of the last launch, kept so a restart (profiling toggle, replay) can re-run with the same input. */
    private volatile @Nullable String lastInputJson;

    /** The input JSON the current trace was launched with, or {@code null}. Survives a session cancel. */
    public @Nullable String lastInputJson() {
        return lastInputJson;
    }

    /** Remember the input JSON a fresh launch was started with. */
    public void rememberInputJson(@Nullable String inputJson) {
        this.lastInputJson = inputJson;
    }

    /** Register a new session, terminating any previous one. */
    public DebugSession start(DebugSession session) {
        touchTrees();
        reaper.register(session);
        var previous = ref.getAndSet(session);
        if (previous != null) {
            reaper.unregister(previous);
            previous.terminate();
        }
        return session;
    }

    /** The active session if it belongs to the given project, otherwise {@code null}. */
    public @Nullable DebugSession find(ProjectId projectId) {
        var session = ref.get();
        if (session == null || !session.getProjectId().equals(projectId)) {
            return null;
        }
        session.touch();
        touchTrees();
        return session;
    }

    /** Terminate and drop the active session. The kept tree stays. */
    public void clear() {
        var session = ref.getAndSet(null);
        if (session != null) {
            reaper.unregister(session);
            session.terminate();
        }
    }

    /** Keep the executed tree of a finished business-view run. */
    public synchronized void keepTree(TraceTreeSnapshot snapshot) {
        trees.values().removeIf(TraceTreeSnapshot::isReleased);
        release(trees.put(snapshot.getRunId(), snapshot));
        reaper.register(snapshot);
    }

    /**
     * The kept tree, if it was recorded by the given run of the given project and is not released yet, otherwise
     * {@code null}.
     */
    public synchronized @Nullable CallNode tree(ProjectId projectId, String runId) {
        var snapshot = trees.get(runId);
        return snapshot == null ? null : snapshot.tree(projectId);
    }

    /** Release the kept tree of the given run. */
    public synchronized void releaseTree(String runId) {
        release(trees.remove(runId));
    }

    /** Release every kept tree. */
    public synchronized void releaseTrees() {
        trees.values().forEach(this::release);
        trees.clear();
    }

    /** The client is at work: its trees are not idle, though no branch of them is read. */
    private synchronized void touchTrees() {
        trees.values().forEach(TraceTreeSnapshot::touch);
    }

    private void release(@Nullable TraceTreeSnapshot snapshot) {
        if (snapshot != null) {
            reaper.unregister(snapshot);
            snapshot.release();
        }
    }

    public Set<String> breakpoints() {
        return Set.copyOf(breakpoints);
    }

    /** Replace the breakpoint set and apply it to the running session, if any. */
    public void setBreakpoints(Collection<String> uris) {
        breakpoints.clear();
        breakpoints.addAll(uris);
        var session = ref.get();
        if (session != null) {
            session.getDebugger().setBreakpoints(breakpoints());
        }
    }

    public Set<String> watches() {
        return Set.copyOf(watches);
    }

    /**
     * Replace the watch set and apply it to the running session, if any, so a cell added mid-debug is
     * captured as stepping reaches it. It cannot recover executions that already happened; a fresh start
     * captures from the beginning of the run.
     */
    public void setWatches(Collection<String> cells) {
        watches.clear();
        watches.addAll(cells);
        var session = ref.get();
        if (session != null) {
            session.getDebugger().setWatches(watches());
        }
    }

    @EventListener
    public void onWorkspaceReset(WorkspaceResetEvent event) {
        try {
            clear();
            releaseTrees();
        } catch (Exception e) {
            log.warn("Failed to clear debug session on workspace reset", e);
        }
    }

    @PreDestroy
    public void onDestroy() {
        try {
            clear();
            releaseTrees();
        } catch (Exception e) {
            log.warn("Failed to clear debug session on destroy", e);
        }
    }
}
