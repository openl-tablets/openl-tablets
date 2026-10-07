package org.openl.studio.compare.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.tree.DiffTreeNodeImpl;

class ComparisonRegistryTest {

    private static final List<Path> FILES = List.of(Path.of("first.xlsx"), Path.of("second.xlsx"));

    private ComparisonFileStore fileStore;
    private ComparisonRegistry registry;

    @BeforeEach
    void setUp() {
        fileStore = mock(ComparisonFileStore.class);
        registry = new ComparisonRegistry(fileStore);
    }

    @Test
    void holdsTheComparisonItWasGiven() {
        var tree = register("cmp-1", CompletableFuture.completedFuture(node()));

        assertTrue(registry.has("cmp-1"));
        assertTrue(registry.isDone("cmp-1"));
        assertSame(tree, registry.result("cmp-1"));
    }

    @Test
    void knowsNothingOfAnotherComparison() {
        register("cmp-1", CompletableFuture.completedFuture(node()));

        assertFalse(registry.has("cmp-2"));
        assertFalse(registry.isDone("cmp-2"));
        assertNull(registry.result("cmp-2"));
    }

    @Test
    void hasNoResultWhileTheComparisonIsRunning() {
        register("cmp-1", new CompletableFuture<>());

        assertTrue(registry.has("cmp-1"));
        assertFalse(registry.isDone("cmp-1"));
        assertNull(registry.result("cmp-1"));
    }

    @Test
    void reportsWhatTheComparisonFailedWith() {
        registry.register("cmp-1", FILES, CompletableFuture.failedFuture(new IllegalStateException("broken")));

        assertTrue(registry.isDone("cmp-1"));
        assertThrows(IllegalStateException.class, () -> registry.result("cmp-1"));
    }

    @Test
    void releasesThePreviousComparisonWhenAnotherOneStarts() {
        register("cmp-1", CompletableFuture.completedFuture(node()));
        register("cmp-2", CompletableFuture.completedFuture(node()));

        assertFalse(registry.has("cmp-1"));
        assertTrue(registry.has("cmp-2"));
        verify(fileStore).delete(FILES);
    }

    @Test
    void keepsTheFilesOfAnAbandonedComparisonUntilItStopsReadingThem() {
        var task = new CompletableFuture<DiffTreeNode>();
        register("cmp-1", task);

        registry.drop("cmp-1");
        assertFalse(registry.has("cmp-1"));
        verify(fileStore, never()).delete(anyCollection());

        task.complete(node());
        verify(fileStore).delete(FILES);
    }

    @Test
    void leavesAComparisonOfAnotherIdentifierAlone() {
        register("cmp-1", CompletableFuture.completedFuture(node()));

        registry.drop("cmp-2");

        assertTrue(registry.has("cmp-1"));
        verify(fileStore, never()).delete(anyCollection());
    }

    @Test
    void releasesTheComparisonWhenTheSessionEnds() {
        register("cmp-1", CompletableFuture.completedFuture(node()));

        registry.clear();

        assertFalse(registry.has("cmp-1"));
        verify(fileStore).delete(FILES);
    }

    private DiffTreeNode register(String id, CompletableFuture<DiffTreeNode> task) {
        registry.register(id, FILES, task);
        return task.isDone() ? task.join() : null;
    }

    private static DiffTreeNode node() {
        return new DiffTreeNodeImpl();
    }
}
