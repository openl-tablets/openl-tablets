package org.openl.studio.compare.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.xls.merge.compare.WorkbookComparison;

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
        assertSame(tree, registry.read("cmp-1", comparison -> comparison));
    }

    @Test
    void knowsNothingOfAnotherComparison() {
        register("cmp-1", CompletableFuture.completedFuture(node()));

        assertFalse(registry.has("cmp-2"));
        assertFalse(registry.isDone("cmp-2"));
        assertNull(registry.read("cmp-2", comparison -> comparison));
    }

    @Test
    void hasNoResultWhileTheComparisonIsRunning() {
        register("cmp-1", new CompletableFuture<>());

        assertTrue(registry.has("cmp-1"));
        assertFalse(registry.isDone("cmp-1"));
        assertNull(registry.read("cmp-1", comparison -> comparison));
    }

    @Test
    void reportsWhatTheComparisonFailedWith() {
        registry.register("cmp-1", FILES, CompletableFuture.failedFuture(new IllegalStateException("broken")));

        assertTrue(registry.isDone("cmp-1"));
        assertThrows(IllegalStateException.class, () -> registry.read("cmp-1", comparison -> comparison));
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
        var task = new CompletableFuture<WorkbookComparison>();
        register("cmp-1", task);

        registry.drop("cmp-1");
        assertFalse(registry.has("cmp-1"));
        verify(fileStore, never()).delete(anyCollection());

        task.complete(node());
        verify(fileStore).delete(FILES);
    }

    @Test
    void closesTheWorkbooksOfAReleasedComparisonBeforeItsFilesAreDeleted() throws IOException {
        var comparison = node();
        register("cmp-1", CompletableFuture.completedFuture(comparison));

        registry.drop("cmp-1");

        var order = inOrder(comparison, fileStore);
        order.verify(comparison).close();
        order.verify(fileStore).delete(FILES);
    }

    @Test
    void closesTheWorkbooksOfAReleasedComparisonOnlyOnceTheRequestReadingThemIsDone() throws Exception {
        var comparison = node();
        register("cmp-1", CompletableFuture.completedFuture(comparison));
        var reading = new CountDownLatch(1);
        var done = new CountDownLatch(1);
        var reader = CompletableFuture.supplyAsync(() -> registry.read("cmp-1", read -> {
            reading.countDown();
            await(done);
            return read;
        }));
        reading.await();

        var dropping = CompletableFuture.runAsync(() -> registry.drop("cmp-1"));

        // The release waits for the request: the workbooks it reads stay open
        assertThrows(TimeoutException.class, () -> dropping.get(200, TimeUnit.MILLISECONDS));
        verify(comparison, never()).close();
        done.countDown();
        assertSame(comparison, reader.get());
        dropping.get();
        verify(comparison).close();
        // A released comparison is read no more
        assertNull(registry.read("cmp-1", read -> read));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void deletesTheFilesOfAComparisonWhoseWorkbooksDoNotClose() throws IOException {
        var comparison = node();
        doThrow(new IOException("locked")).when(comparison).close();
        register("cmp-1", CompletableFuture.completedFuture(comparison));

        registry.drop("cmp-1");

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

    private WorkbookComparison register(String id, CompletableFuture<WorkbookComparison> task) {
        registry.register(id, FILES, task);
        return task.isDone() ? task.join() : null;
    }

    private static WorkbookComparison node() {
        return mock(WorkbookComparison.class);
    }
}
