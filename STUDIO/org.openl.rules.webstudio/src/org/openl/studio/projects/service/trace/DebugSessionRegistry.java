package org.openl.studio.projects.service.trace;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Session-scoped holder for the user's active debug session and breakpoints.
 *
 * <p>Holds at most one debug session; starting a new one terminates the previous. Breakpoints persist
 * across runs so the user can set them before starting and they apply to the next run. Everything is
 * released when a new session starts, on explicit cancel, when the workspace is reset, or when the HTTP
 * session is destroyed.
 */
@Slf4j
@Component
@SessionScope
@RequiredArgsConstructor
public class DebugSessionRegistry {

    private final AtomicReference<DebugSession> ref = new AtomicReference<>();
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
        reaper.register(session);
        DebugSession previous = ref.getAndSet(session);
        if (previous != null) {
            reaper.unregister(previous);
            previous.terminate();
        }
        return session;
    }

    /** The active session if it belongs to the given project, otherwise {@code null}. */
    public @Nullable DebugSession find(ProjectIdModel projectId) {
        DebugSession session = ref.get();
        if (session == null || !session.getProjectId().equals(projectId)) {
            return null;
        }
        session.touch();
        return session;
    }

    /** Terminate and drop the active session. */
    public void clear() {
        DebugSession session = ref.getAndSet(null);
        if (session != null) {
            reaper.unregister(session);
            session.terminate();
        }
    }

    public Set<String> breakpoints() {
        return Set.copyOf(breakpoints);
    }

    /** Replace the breakpoint set and apply it to the running session, if any. */
    public void setBreakpoints(Collection<String> uris) {
        breakpoints.clear();
        breakpoints.addAll(uris);
        DebugSession session = ref.get();
        if (session != null) {
            session.getDebugger().setBreakpoints(breakpoints());
        }
    }

    public Set<String> watches() {
        return Set.copyOf(watches);
    }

    /**
     * Replace the watch set. Applied on the next start, not to a running session: a watch captures from
     * the beginning of a run, so adding one mid-run cannot recover the executions that already happened.
     */
    public void setWatches(Collection<String> cells) {
        watches.clear();
        watches.addAll(cells);
    }

    @EventListener
    public void onWorkspaceReset(WorkspaceResetEvent event) {
        try {
            clear();
        } catch (Exception e) {
            log.warn("Failed to clear debug session on workspace reset", e);
        }
    }

    @PreDestroy
    public void onDestroy() {
        try {
            clear();
        } catch (Exception e) {
            log.warn("Failed to clear debug session on destroy", e);
        }
    }
}
