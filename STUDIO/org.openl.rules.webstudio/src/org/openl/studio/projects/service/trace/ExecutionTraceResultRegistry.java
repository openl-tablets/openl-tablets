package org.openl.studio.projects.service.trace;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Session-scoped registry for managing trace execution tasks.
 * <p>
 * This registry holds at most one trace execution task per user session.
 * When a new trace is started, any previously running trace is automatically
 * cancelled to prevent resource exhaustion.
 * </p>
 * <p>
 * The registry stores:
 * <ul>
 *   <li>The {@link CompletableFuture} representing the asynchronous trace task</li>
 *   <li>The {@link TraceHelper} containing cached trace results for lazy loading</li>
 *   <li>Project and table identifiers for validation</li>
 * </ul>
 * </p>
 * <p>
 * Uses {@link ScopedProxyMode#TARGET_CLASS} to allow injection into singleton
 * beans while maintaining session-scope semantics.
 * </p>
 *
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionTraceResultRegistry {

    private record Entry(ProjectIdModel projectId,
                         String tableId,
                         CompletableFuture<ITracerObject> task,
                         TraceHelper traceHelper) {
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    /**
     * Register a new trace task; cancels previous one if running.
     *
     * @param projectId   the project identifier
     * @param tableId     the table identifier
     * @param task        the trace execution future
     * @param traceHelper the trace helper for caching results
     */
    public void setTask(ProjectIdModel projectId,
                        String tableId,
                        CompletableFuture<ITracerObject> task,
                        TraceHelper traceHelper) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(tableId, "tableId");
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(traceHelper, "traceHelper");

        Entry previous = ref.getAndSet(new Entry(projectId, tableId, task, traceHelper));
        if (previous != null && !previous.task.isDone()) {
            previous.task.cancel(true);
        }
    }

    /**
     * Stop current task if running.
     */
    public void cancelIfAny() {
        Entry e = ref.get();
        if (e != null) {
            e.task().cancel(true);
        }
    }

    /**
     * Clear the registry, releasing the trace data from memory.
     * Cancels the task if it's still running.
     */
    public void clear() {
        Entry e = ref.getAndSet(null);
        if (e != null && !e.task().isDone()) {
            e.task().cancel(true);
        }
    }

    /**
     * Check if the current task for the given project exists.
     *
     * @param projectId the project identifier
     * @return true if a task exists for this project
     */
    public boolean hasTask(ProjectIdModel projectId) {
        Entry e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    /**
     * Check if the task for the given project is completed.
     *
     * @param projectId the project identifier
     * @return true if the task is completed
     */
    public boolean isDone(ProjectIdModel projectId) {
        Entry e = ref.get();
        return e != null && e.projectId().equals(projectId) && e.task().isDone();
    }

    /**
     * Return the trace helper if the task completed successfully.
     * Returns null if the task is not done or failed/cancelled.
     *
     * @param projectId the project identifier
     * @return the trace helper with cached results, or null
     */
    public TraceHelper getTraceHelperIfDone(ProjectIdModel projectId) {
        Entry e = ref.get();
        if (e == null || !e.projectId().equals(projectId)) {
            return null;
        }

        CompletableFuture<ITracerObject> future = e.task();
        if (!future.isDone() || future.isCancelled()) {
            return null;
        }

        try {
            future.get(); // Ensure task completed successfully
            return e.traceHelper();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }
}
