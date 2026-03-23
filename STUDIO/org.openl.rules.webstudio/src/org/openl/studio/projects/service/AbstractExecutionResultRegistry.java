package org.openl.studio.projects.service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Abstract session-scoped registry for managing asynchronous execution tasks.
 * <p>
 * Holds at most one execution task at a time. When a new task is registered,
 * any previously running task is automatically cancelled to prevent resource exhaustion.
 * </p>
 *
 * @param <T> the result type of the execution task
 */
public abstract class AbstractExecutionResultRegistry<T> {

    private record Entry<T>(ProjectIdModel projectId,
                            String tableId,
                            CompletableFuture<T> task) {
    }

    private final AtomicReference<Entry<T>> ref = new AtomicReference<>();

    /**
     * Register a new task; cancels previous one if still running.
     *
     * @param projectId the project identifier (must not be null)
     * @param tableId   the table identifier (may be null)
     * @param task      the execution future (must not be null)
     */
    protected void registerTask(ProjectIdModel projectId,
                                String tableId,
                                CompletableFuture<T> task) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(task, "task");

        Entry<T> previous = ref.getAndSet(new Entry<>(projectId, tableId, task));
        if (previous != null && !previous.task.isDone()) {
            previous.task.cancel(true);
        }
    }

    /**
     * Stop current task if running.
     */
    public void cancelIfAny() {
        Entry<T> e = ref.get();
        if (e != null) {
            e.task().cancel(true);
        }
    }

    /**
     * Clear the registry, releasing data from memory.
     * Cancels the task if it's still running.
     */
    public void clear() {
        Entry<T> e = ref.getAndSet(null);
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
        Entry<T> e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    /**
     * Check if the task for the given project is completed.
     *
     * @param projectId the project identifier
     * @return true if the task is completed
     */
    public boolean isDone(ProjectIdModel projectId) {
        Entry<T> e = ref.get();
        return e != null && e.projectId().equals(projectId) && e.task().isDone();
    }

    /**
     * Return the result if the task completed successfully.
     * Returns null if the task is not done or failed/cancelled.
     *
     * @param projectId the project identifier
     * @return the result, or null
     */
    public T getResultIfDone(ProjectIdModel projectId) {
        Entry<T> e = ref.get();
        if (e == null || !e.projectId().equals(projectId)) {
            return null;
        }

        CompletableFuture<T> future = e.task();
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
