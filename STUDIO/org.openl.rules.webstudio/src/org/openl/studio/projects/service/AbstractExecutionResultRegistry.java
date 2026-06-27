package org.openl.studio.projects.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Abstract session-scoped registry for managing asynchronous execution tasks.
 * <p>
 * Holds at most one execution task per project, so several projects opened in one session (parallel
 * multi-project editing, multiple browser tabs) can run and keep their results independently. Registering a new
 * task for a project cancels only that project's previous task.
 * </p>
 *
 * @param <T> the result type of the execution task
 */
@Slf4j
public abstract class AbstractExecutionResultRegistry<T> {

    private record Entry<T>(ProjectIdModel projectId,
                            String tableId,
                            CompletableFuture<T> task) {
    }

    private final Map<ProjectIdModel, Entry<T>> entries = new ConcurrentHashMap<>();

    /**
     * Register a new task for a project; cancels that project's previous task if still running.
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

        cancel(entries.put(projectId, new Entry<>(projectId, tableId, task)));
    }

    /**
     * Stop the given project's task if running, leaving other projects' tasks untouched.
     */
    public void cancel(ProjectIdModel projectId) {
        Entry<T> e = entries.get(projectId);
        if (e != null) {
            e.task().cancel(true);
        }
    }

    /**
     * Drop the given project's task, releasing its data from memory. Cancels it if still running.
     */
    public void clear(ProjectIdModel projectId) {
        cancel(entries.remove(projectId));
    }

    /**
     * Drop every project's task, releasing data from memory. Cancels any still running.
     */
    public void clear() {
        entries.keySet().forEach(this::clear);
    }

    private void cancel(Entry<T> entry) {
        if (entry != null && !entry.task().isDone()) {
            entry.task().cancel(true);
        }
    }

    /**
     * Drop cached execution results when the session workspace is reset: results computed
     * against the previous compiled state are no longer valid.
     */
    @EventListener
    public void onWorkspaceReset(@NonNull WorkspaceResetEvent event) {
        try {
            clear();
        } catch (Exception e) {
            log.warn("onWorkspaceReset failed", e);
        }
    }

    /**
     * Check if a task for the given project exists.
     *
     * @param projectId the project identifier
     * @return true if a task exists for this project
     */
    public boolean hasTask(ProjectIdModel projectId) {
        return entries.containsKey(projectId);
    }

    /**
     * Check if the task for the given project is completed.
     *
     * @param projectId the project identifier
     * @return true if the task is completed
     */
    public boolean isDone(ProjectIdModel projectId) {
        Entry<T> e = entries.get(projectId);
        return e != null && e.task().isDone();
    }

    /**
     * Return the result if the task completed successfully.
     * Returns null if the task is not done or failed/cancelled.
     *
     * @param projectId the project identifier
     * @return the result, or null
     */
    public T getResultIfDone(ProjectIdModel projectId) {
        Entry<T> e = entries.get(projectId);
        if (e == null) {
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
