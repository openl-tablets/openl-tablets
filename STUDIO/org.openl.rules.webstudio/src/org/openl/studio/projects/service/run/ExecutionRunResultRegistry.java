package org.openl.studio.projects.service.run;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Session-scoped registry for managing run execution tasks.
 * <p>
 * This registry holds at most one run execution task per user session.
 * When a new run is started, any previously running task is automatically
 * cancelled to prevent resource exhaustion.
 * </p>
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionRunResultRegistry {

    private record Entry(ProjectIdModel projectId,
                         String tableId,
                         CompletableFuture<TestUnitsResults> task) {
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    /**
     * Register a new run task; cancels previous one if running.
     *
     * @param projectId the project identifier
     * @param tableId   the table identifier
     * @param task      the run execution future
     */
    public void setTask(ProjectIdModel projectId,
                        String tableId,
                        CompletableFuture<TestUnitsResults> task) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(tableId, "tableId");
        Objects.requireNonNull(task, "task");

        Entry previous = ref.getAndSet(new Entry(projectId, tableId, task));
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
     * Clear the registry, releasing data from memory.
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
     * Return the result if the task completed successfully.
     * Returns null if the task is not done or failed/cancelled.
     *
     * @param projectId the project identifier
     * @return the test results, or null
     */
    public TestUnitsResults getResultIfDone(ProjectIdModel projectId) {
        Entry e = ref.get();
        if (e == null || !e.projectId().equals(projectId)) {
            return null;
        }

        CompletableFuture<TestUnitsResults> future = e.task();
        if (!future.isDone() || future.isCancelled()) {
            return null;
        }

        try {
            return future.get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }
}
