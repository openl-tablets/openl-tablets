package org.openl.rules.ui;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Single compile cycle registered by {@link ProjectModel} when
 * {@link ProjectModel#compileProject(boolean, boolean)} starts. The cycle owns a
 * {@link CompletableFuture} that completes (successfully or exceptionally) when the
 * underlying asynchronous compilation finishes, and records the instant the cycle was
 * registered so observers can report an accurate compile duration regardless of when they
 * began watching.
 *
 * <p>External observers (e.g. {@code CompilationJobImpl}) can use object identity or
 * {@link #id()} to detect when {@link ProjectModel} has registered a new cycle and the
 * previously held reference is stale.
 *
 * @param id        monotonically increasing identifier within a single {@code ProjectModel}
 * @param startedAt instant when this cycle was registered (i.e. when {@code compileProject}
 *                  began)
 * @param future    future that completes when this specific cycle finishes
 */
public record RegisteredCompilation(long id, Instant startedAt, CompletableFuture<Void> future) {

    RegisteredCompilation(long id) {
        this(id, Instant.now(), new CompletableFuture<>());
    }

    /**
     * Factory for an already-completed cycle, used as a sentinel before any compilation has
     * been registered.
     */
    public static RegisteredCompilation completed(long id) {
        return new RegisteredCompilation(id, Instant.now(), CompletableFuture.completedFuture(null));
    }
}
