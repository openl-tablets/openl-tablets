package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.ProjectIdModel;

class AbstractExecutionResultRegistryTest {

    private static class TestRegistry extends AbstractExecutionResultRegistry<String> {
        public void setTask(ProjectIdModel projectId, String tableId, CompletableFuture<String> task) {
            registerTask(projectId, tableId, task);
        }
    }

    private TestRegistry registry;
    private ProjectIdModel projectId;
    private ProjectIdModel otherProjectId;

    @BeforeEach
    void setUp() {
        registry = new TestRegistry();
        projectId = ProjectIdModel.builder()
                .repository("design")
                .projectName("TestProject")
                .build();
        otherProjectId = ProjectIdModel.builder()
                .repository("design")
                .projectName("OtherProject")
                .build();
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

    @Test
    void registerTask_differentProjectsCoexist() {
        var taskA = new CompletableFuture<String>();
        var taskB = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", taskA);
        registry.setTask(otherProjectId, "t1", taskB);

        // Registering B must NOT cancel A's running task; both results live side by side.
        assertFalse(taskA.isCancelled());
        assertTrue(registry.hasTask(projectId));
        assertTrue(registry.hasTask(otherProjectId));

        taskA.complete("A");
        taskB.complete("B");
        assertEquals("A", registry.getResultIfDone(projectId));
        assertEquals("B", registry.getResultIfDone(otherProjectId));
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

    // --- cancel(projectId) ---

    @Test
    void cancel_empty() {
        // Should not throw
        registry.cancel(projectId);
    }

    @Test
    void cancel_runningTask() {
        var task = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", task);

        registry.cancel(projectId);

        assertTrue(task.isCancelled());
        // Task still registered (not cleared)
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void cancel_completedTask() {
        var task = CompletableFuture.completedFuture("done");
        registry.setTask(projectId, "t1", task);

        registry.cancel(projectId);

        // Already completed, cancel has no effect
        assertFalse(task.isCancelled());
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void cancel_leavesOtherProjectUntouched() {
        var taskA = new CompletableFuture<String>();
        var taskB = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", taskA);
        registry.setTask(otherProjectId, "t1", taskB);

        registry.cancel(projectId);

        assertTrue(taskA.isCancelled());
        assertFalse(taskB.isCancelled());
        assertTrue(registry.hasTask(otherProjectId));
    }

    // --- clear(projectId) ---

    @Test
    void clearProject_removesOnlyThatProject() {
        var taskA = new CompletableFuture<String>();
        var taskB = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", taskA);
        registry.setTask(otherProjectId, "t1", taskB);

        registry.clear(projectId);

        assertTrue(taskA.isCancelled());
        assertFalse(registry.hasTask(projectId));
        assertFalse(taskB.isCancelled());
        assertTrue(registry.hasTask(otherProjectId));
    }

    // --- clear() ---

    @Test
    void clear_empty() {
        // Should not throw
        registry.clear();
    }

    @Test
    void clear_cancelsAndRemovesEveryProject() {
        var taskA = new CompletableFuture<String>();
        var taskB = new CompletableFuture<String>();
        registry.setTask(projectId, "t1", taskA);
        registry.setTask(otherProjectId, "t1", taskB);

        registry.clear();

        assertTrue(taskA.isCancelled());
        assertTrue(taskB.isCancelled());
        assertFalse(registry.hasTask(projectId));
        assertFalse(registry.hasTask(otherProjectId));
    }

    @Test
    void clear_completedTask() {
        var task = CompletableFuture.completedFuture("done");
        registry.setTask(projectId, "t1", task);

        registry.clear();

        assertFalse(task.isCancelled());
        assertFalse(registry.hasTask(projectId));
    }
}
