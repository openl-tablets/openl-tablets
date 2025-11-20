package org.openl.studio.projects.service.tests;

import java.util.List;
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

@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionTestsResultRegistry {

    private record Entry(ProjectIdModel projectId,
                         CompletableFuture<List<TestUnitsResults>> task) {
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    /**
     * Register a new task; cancels previous one if running.
     */
    public void setTask(ProjectIdModel projectId, CompletableFuture<List<TestUnitsResults>> task) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(task, "task");

        Entry previous = ref.getAndSet(new Entry(projectId, task));
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
     */
    public boolean hasTask(ProjectIdModel projectId) {
        Entry e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    /**
     * Check if the task for the given project is completed.
     */
    public boolean isDone(ProjectIdModel projectId) {
        Entry e = ref.get();
        return e != null && e.projectId().equals(projectId) && e.task().isDone();
    }

    /**
     * Return the result of the task if completed successfully.
     * Returns null if the task is not done or failed/cancelled.
     */
    public List<TestUnitsResults> getResultIfDone(ProjectIdModel projectId) {
        Entry e = ref.get();
        if (e == null || !e.projectId().equals(projectId)) {
            return null;
        }

        CompletableFuture<List<TestUnitsResults>> future = e.task();
        if (!future.isDone() || future.isCancelled()) {
            return null;
        }

        try {
            return future.get(); // safe since isDone() == true
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }
}
