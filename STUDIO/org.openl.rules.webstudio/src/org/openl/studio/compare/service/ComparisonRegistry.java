package org.openl.studio.compare.service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * The comparison a session is working on.
 *
 * <p>A session holds one comparison at a time: a comparison keeps both workbooks parsed, which a
 * screen showing one comparison does not need twice over. Starting another one releases the
 * previous, and so does the end of the session.
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class ComparisonRegistry {

    private record Entry(String id, List<Path> files, CompletableFuture<DiffTreeNode> task) {
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
    public void register(String id, List<Path> files, CompletableFuture<DiffTreeNode> task) {
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
        return entry != null && entry.id().equals(id);
    }

    /**
     * Tells whether the named comparison has finished, successfully or not.
     *
     * @param id identifier of the comparison
     * @return true when it is this session's comparison and it has finished
     */
    public boolean isDone(String id) {
        var entry = current.get();
        return entry != null && entry.id().equals(id) && entry.task().isDone();
    }

    /**
     * The result of the named comparison.
     *
     * @param id identifier of the comparison
     * @return what the comparison found, or null when this session holds no such finished comparison
     * @throws RuntimeException what the comparison failed with
     */
    public @Nullable DiffTreeNode result(String id) {
        var entry = current.get();
        if (entry == null || !entry.id().equals(id)) {
            return null;
        }
        var task = entry.task();
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
        if (entry != null && entry.id().equals(id) && current.compareAndSet(entry, null)) {
            release(entry);
        }
    }

    @PreDestroy
    void clear() {
        release(current.getAndSet(null));
    }

    /**
     * Lets a comparison go, deleting its files once it stops reading them. A comparison the user has
     * abandoned still runs to its end: only its own thread can put down the workbooks it parsed.
     */
    private void release(@Nullable Entry entry) {
        if (entry != null) {
            entry.task().whenComplete((result, error) -> fileStore.delete(entry.files()));
        }
    }
}
