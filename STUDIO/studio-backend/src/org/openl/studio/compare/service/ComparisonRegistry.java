package org.openl.studio.compare.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import jakarta.annotation.PreDestroy;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.xls.merge.compare.WorkbookComparison;
import org.openl.studio.session.ClientSessionScope;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * The comparison a client is working on: a browser session, or the credentials of a stateless client.
 *
 * <p>A client holds one comparison at a time: a comparison keeps both workbooks open, which a
 * screen showing one comparison does not need twice over. Starting another one releases the
 * previous, and so does the end of the client.
 *
 * <p>A comparison is read while it is held only: a request reading it keeps its workbooks open until it is done, so a
 * comparison released meanwhile is closed after that request.
 */
@Slf4j
@Component
@ClientSessionScope
@RequiredArgsConstructor
public class ComparisonRegistry {

    /** A comparison held, with the lease the requests reading it take and whether it was closed. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Entry {
        private final String id;
        private final List<Path> files;
        private final CompletableFuture<WorkbookComparison> task;
        private final ReadWriteLock lease = new ReentrantReadWriteLock();
        /** Whether the workbooks are closed; written and read under the lease. */
        private boolean closed;

        private boolean is(String other) {
            return id.equals(other);
        }
    }

    private final ComparisonFileStore fileStore;

    private final AtomicReference<Entry> current = new AtomicReference<>();

    /**
     * Takes a started comparison as the one of this session, releasing the previous one.
     *
     * @param id    identifier of the comparison
     * @param files the compared files, deleted when the comparison is released
     * @param task  the comparison itself
     */
    public void register(String id, List<Path> files, CompletableFuture<WorkbookComparison> task) {
        release(current.getAndSet(new Entry(id, List.copyOf(files), task)));
    }

    /**
     * Tells whether the named comparison is the one this session holds.
     *
     * @param id identifier of the comparison
     * @return true when this session holds it
     */
    public boolean has(String id) {
        var entry = current.get();
        return entry != null && entry.is(id);
    }

    /**
     * Tells whether the named comparison has finished, successfully or not.
     *
     * @param id identifier of the comparison
     * @return true when it is this session's comparison and it has finished
     */
    public boolean isDone(String id) {
        var entry = current.get();
        return entry != null && entry.is(id) && entry.task.isDone();
    }

    /**
     * Reads what the named comparison found. Its workbooks stay open while the reader runs, even when the comparison
     * is released meanwhile.
     *
     * @param id     identifier of the comparison
     * @param reader reads what the comparison found
     * @return what the reader made of the comparison, or null when this session holds no such finished comparison
     * @throws RuntimeException what the comparison failed with
     */
    public <T> @Nullable T read(String id, Function<? super WorkbookComparison, T> reader) {
        var entry = current.get();
        if (entry == null || !entry.is(id)) {
            return null;
        }
        var comparison = resultOf(entry);
        if (comparison == null) {
            return null;
        }
        var lease = entry.lease.readLock();
        lease.lock();
        try {
            return entry.closed ? null : reader.apply(comparison);
        } finally {
            lease.unlock();
        }
    }

    private static @Nullable WorkbookComparison resultOf(Entry entry) {
        var task = entry.task;
        if (!task.isDone() || task.isCancelled()) {
            return null;
        }
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            // The caller asked for the comparison, not for the plumbing that ran it: report what it
            // failed with rather than the wrapper the future kept it in.
            throw RuntimeExceptionWrapper.wrap(e.getCause() == null ? e : e.getCause());
        }
    }

    /**
     * Releases the named comparison, when this session still holds it.
     *
     * @param id identifier of the comparison
     */
    public void drop(String id) {
        var entry = current.get();
        if (entry != null && entry.is(id) && current.compareAndSet(entry, null)) {
            release(entry);
        }
    }

    @PreDestroy
    void clear() {
        release(current.getAndSet(null));
    }

    /**
     * Lets a comparison go, closing the workbooks it holds open and deleting its files once it stops reading
     * them. A comparison the user has abandoned still runs to its end: only its own thread can put down the
     * workbooks it reads. A request still reading the comparison finishes before the workbooks are closed.
     */
    private void release(@Nullable Entry entry) {
        if (entry != null) {
            entry.task.whenComplete((result, error) -> {
                var lease = entry.lease.writeLock();
                lease.lock();
                try {
                    entry.closed = true;
                    if (result != null) {
                        close(result);
                    }
                } finally {
                    lease.unlock();
                }
                fileStore.delete(entry.files);
            });
        }
    }

    private static void close(WorkbookComparison comparison) {
        try {
            comparison.close();
        } catch (IOException e) {
            // Nothing is left to read from a released comparison: a workbook that does not close is only logged.
            log.warn("Cannot close the workbooks of a comparison", e);
        }
    }
}
