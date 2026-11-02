package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.studio.common.exception.GoneException;

class AbstractExecutionResultRegistryTest {

    private static class TestRegistry extends AbstractExecutionResultRegistry<String> {
        public void setTask(ProjectId projectId, String tableId, CompletableFuture<String> task) {
            registerTask(projectId, tableId, task);
        }
    }

    private TestRegistry registry;
    private ProjectId projectId;
    private ProjectId otherProjectId;

    @BeforeEach
    void setUp() {
        registry = new TestRegistry();
        projectId = ProjectIds.of("design", "TestProject");
        otherProjectId = ProjectIds.of("design", "OtherProject");
    }

    // --- registerTask ---

    @Test
    void registerTask_nullProjectId() {
        var task = CompletableFuture.completedFuture("ok");
        assertThrows(NullPointerException.class, () -> registry.setTask(null, "t1", task));
    }

    @Test
    void registerTask_nullTask() {
        assertThrows(NullPointerException.class, () -> registry.setTask(projectId, "t1", null));
    }

    @Test
    void registerTask_replacesAndCancelsPrevious() {
        var first = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", first);

        assertFalse(first.isDone());

        var second = CompletableFuture.completedFuture("result");
        registry.setTask(projectId, "t2", second);

        assertTrue(first.isCancelled());
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void registerTask_doesNotCancelCompletedPrevious() {
        var first = CompletableFuture.completedFuture("done");
        registry.setTask(projectId, "t1", first);

        var second = CompletableFuture.completedFuture("result");
        registry.setTask(projectId, "t2", second);

        // first was already done, should not be cancelled
        assertFalse(first.isCancelled());
        assertTrue(first.isDone());
    }

    // --- hasTask ---

    @Test
    void hasTask_empty() {
        assertFalse(registry.hasTask(projectId));
    }

    @Test
    void hasTask_matchingProject() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("ok"));
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void hasTask_differentProject() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("ok"));
        assertFalse(registry.hasTask(otherProjectId));
    }

    // --- isDone ---

    @Test
    void isDone_empty() {
        assertFalse(registry.isDone(projectId));
    }

    @Test
    void isDone_completed() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("ok"));
        assertTrue(registry.isDone(projectId));
    }

    @Test
    void isDone_notCompleted() {
        registry.setTask(projectId, "t1", new CompletableFuture<>());
        assertFalse(registry.isDone(projectId));
    }

    @Test
    void isDone_differentProject() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("ok"));
        assertFalse(registry.isDone(otherProjectId));
    }

    // --- getResultIfDone ---

    @Test
    void getResultIfDone_empty() {
        assertNull(registry.getResultIfDone(projectId));
    }

    @Test
    void getResultIfDone_completed() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("result"));
        assertEquals("result", registry.getResultIfDone(projectId));
    }

    @Test
    void getResultIfDone_differentProject() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("result"));
        assertNull(registry.getResultIfDone(otherProjectId));
    }

    @Test
    void getResultIfDone_notCompleted() {
        registry.setTask(projectId, "t1", new CompletableFuture<>());
        assertNull(registry.getResultIfDone(projectId));
    }

    @Test
    void getResultIfDone_cancelled() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);
        task.cancel(true);

        assertNull(registry.getResultIfDone(projectId));
    }

    @Test
    void getResultIfDone_failed() {
        var task = CompletableFuture.<String>failedFuture(new RuntimeException("boom"));
        registry.setTask(projectId, "t1", task);

        assertThrows(RuntimeException.class, () -> registry.getResultIfDone(projectId));
    }

    // --- cancelIfAny ---

    @Test
    void cancelIfAny_empty() {
        assertDoesNotThrow(registry::cancelIfAny);
        assertFalse(registry.hasTask(projectId));
    }

    @Test
    void cancelIfAny_runningTask() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.cancelIfAny();

        assertTrue(task.isCancelled());
        // Task still registered (not cleared)
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void cancelIfAny_completedTask() {
        var task = CompletableFuture.completedFuture("done");
        registry.setTask(projectId, "t1", task);

        registry.cancelIfAny();

        // Already completed, cancel has no effect
        assertFalse(task.isCancelled());
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void cancel_stopsTheTaskOfTheProject() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.cancel(projectId);

        assertTrue(task.isCancelled());
    }

    @Test
    void cancel_leavesTheTaskOfAnotherProjectGoing() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.cancel(otherProjectId);

        assertFalse(task.isCancelled());
    }

    // --- clear ---

    @Test
    void clearOfTheProject_forgetsAndStopsItsTask() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.clear(projectId);

        assertTrue(task.isCancelled());
        assertFalse(registry.hasTask(projectId));
    }

    @Test
    void clearOfAnotherProject_leavesTheTaskGoing() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.clear(otherProjectId);

        assertFalse(task.isCancelled());
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void clear_empty() {
        assertDoesNotThrow(() -> registry.clear());
        assertFalse(registry.hasTask(projectId));
    }

    @Test
    void clear_runningTask() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.clear();

        assertTrue(task.isCancelled());
        assertFalse(registry.hasTask(projectId));
    }

    @Test
    void clear_completedTask() {
        var task = CompletableFuture.completedFuture("done");
        registry.setTask(projectId, "t1", task);

        registry.clear();

        assertFalse(task.isCancelled());
        assertFalse(registry.hasTask(projectId));
    }

    // --- workspace reset ---

    @Test
    void workspaceReset_marksTheResultsOfTheProjectOutdated() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.onWorkspaceReset(new WorkspaceResetEvent(this));

        assertTrue(task.isCancelled());
        assertFalse(registry.hasTask(projectId));
        assertTrue(registry.isOutdated(projectId));
        assertFalse(registry.isOutdated(otherProjectId));
        var gone = assertThrows(GoneException.class, () -> registry.refuseIfOutdated(projectId));
        assertEquals("openl.error.410.execution.outdated.message", gone.getErrorCode());
        assertDoesNotThrow(() -> registry.refuseIfOutdated(otherProjectId));
    }

    @Test
    void resetOfAnotherProject_keepsTheExecution() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.onWorkspaceReset(new WorkspaceResetEvent(this, otherProjectId));

        assertFalse(task.isCancelled());
        assertTrue(registry.hasTask(projectId));
        assertFalse(registry.isOutdated(projectId));
    }

    @Test
    void resetOfTheProject_dropsItsExecution() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.onWorkspaceReset(new WorkspaceResetEvent(this, projectId));

        assertTrue(task.isCancelled());
        assertTrue(registry.isOutdated(projectId));
    }

    @Test
    void workspaceReset_marksNothingWithoutAnExecution() {
        registry.onWorkspaceReset(new WorkspaceResetEvent(this));

        assertFalse(registry.isOutdated(projectId));
    }

    @Test
    void registerTask_forgetsThatTheResultsWereOutdated() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture("done"));
        registry.onWorkspaceReset(new WorkspaceResetEvent(this));

        registry.setTask(projectId, "t1", new CompletableFuture<>());

        assertFalse(registry.isOutdated(projectId));
    }
}
