package org.openl.studio.projects.service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.rules.workspace.ProjectId;
import org.openl.studio.common.exception.GoneException;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Abstract client-scoped registry for managing asynchronous execution tasks.
 * <p>
 * Holds at most one execution task at a time. When a new task is registered,
 * any previously running task is automatically cancelled to prevent resource exhaustion.
 * </p>
 * <p>
 * Compiling the project again drops the task and what it produced, and the registry remembers that it did, so a
 * reader is told why there is nothing to read rather than that there never was. Registering the next task forgets it.
 * A task registered while the project is compiled again is either dropped with it or kept whole, never marked as
 * dropped while it goes on.
 * </p>
 *
 * @param <T> the result type of the execution task
 */
@Slf4j
public abstract class AbstractExecutionResultRegistry<T> {

    private record Entry<T>(ProjectId projectId,
                            String tableId,
                            CompletableFuture<T> task) {
    }

    private final AtomicReference<Entry<T>> ref = new AtomicReference<>();
    private final AtomicReference<@Nullable ProjectId> outdated = new AtomicReference<>();

    /**
     * Register a new task; cancels previous one if still running.
     *
     * @param projectId the project identifier (must not be null)
     * @param tableId   the table identifier (may be null)
     * @param task      the execution future (must not be null)
     */
    protected synchronized void registerTask(ProjectId projectId,
                                String tableId,
                                CompletableFuture<T> task) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(task, "task");

        outdated.set(null);
        var previous = ref.<T>getAndSet(new Entry<T>(projectId, tableId, task));
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
     * Stops the task of the project, if it is running. A task of another project goes on.
     *
     * @param projectId the project whose task is stopped
     */
    public void cancel(ProjectId projectId) {
        Entry<T> e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            e.task().cancel(true);
        }
    }

    /**
     * Clear the registry, releasing data from memory.
     * Cancels the task if it's still running.
     */
    public synchronized void clear() {
        var e = ref.<T>getAndSet(null);
        if (e != null && !e.task().isDone()) {
            e.task().cancel(true);
        }
    }

    /**
     * Forgets the task of the project and what it produced, stopping it if it is running. A task of another project
     * is left as it is.
     *
     * @param projectId the project whose task is forgotten
     */
    public synchronized void clear(ProjectId projectId) {
        var e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            clear();
        }
    }

    /**
     * Drop cached execution results when the session workspace is reset: results computed
     * against the previous compiled state are no longer valid. An execution still going on is stopped.
     * An execution of another project than the one reset is kept, with what it produced.
     */
    @EventListener
    public synchronized void onWorkspaceReset(@NonNull WorkspaceResetEvent event) {
        try {
            var dropped = ref.get();
            if (dropped == null || !event.concerns(dropped.projectId())) {
                // Nothing to drop, or the execution of another project: it stands as it is.
                return;
            }
            clear();
            outdated.set(dropped.projectId());
        } catch (Exception e) {
            log.warn("onWorkspaceReset failed", e);
        }
    }

    /**
     * Tells whether the last execution of the project was dropped because the project was compiled again.
     *
     * @param projectId the project identifier
     * @return true until the next execution is registered
     */
    public boolean isOutdated(ProjectId projectId) {
        return projectId.equals(outdated.get());
    }

    /**
     * Refuses a read of the results of an execution of the project that was dropped because the project was
     * compiled again.
     *
     * @param projectId the project identifier
     * @throws GoneException {@code execution.outdated.message} until the next execution is registered
     */
    public void refuseIfOutdated(ProjectId projectId) {
        if (isOutdated(projectId)) {
            throw new GoneException("execution.outdated.message");
        }
    }

    /**
     * Check if the current task for the given project exists.
     *
     * @param projectId the project identifier
     * @return true if a task exists for this project
     */
    public boolean hasTask(ProjectId projectId) {
        Entry<T> e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    /**
     * Check if the task for the given project is completed.
     *
     * @param projectId the project identifier
     * @return true if the task is completed
     */
    public boolean isDone(ProjectId projectId) {
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
    public @Nullable T getResultIfDone(ProjectId projectId) {
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
