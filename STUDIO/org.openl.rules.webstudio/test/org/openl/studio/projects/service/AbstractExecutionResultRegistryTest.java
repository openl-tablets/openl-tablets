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
        // Should not throw
        registry.cancelIfAny();
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

    // --- clear ---

    @Test
    void clear_empty() {
        // Should not throw
        registry.clear();
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
}
