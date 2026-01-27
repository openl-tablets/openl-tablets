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
 * Session-scoped registry for trace execution tasks.
 * Holds one trace task per session, automatically cancels previous task when a new one is started.
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

    /**
     * Get the table ID of the current trace task.
     *
     * @param projectId the project identifier
     * @return the table ID, or null if no task exists
     */
    public String getTableId(ProjectIdModel projectId) {
        Entry e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            return e.tableId();
        }
        return null;
    }
}
