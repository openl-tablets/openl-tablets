package org.openl.rules.ui;

import java.util.concurrent.CompletableFuture;

/**
 * Single compile cycle registered by {@link ProjectModel} when
 * {@link ProjectModel#compileProject(boolean, boolean)} starts. The cycle owns a
 * {@link CompletableFuture} that completes (successfully or exceptionally) when the
 * underlying asynchronous compilation finishes.
 *
 * <p>External observers (e.g. {@code CompilationJobImpl}) use object identity to detect
 * when {@link ProjectModel} has registered a new cycle and the previously held reference
 * is stale.
 *
 * @param future future that completes when this specific cycle finishes
 */
public record RegisteredCompilation(CompletableFuture<Void> future) {

    RegisteredCompilation() {
        this(new CompletableFuture<>());
    }

    /**
     * Factory for an already-completed cycle, used as a sentinel before any compilation has
     * been registered.
     */
    public static RegisteredCompilation completed() {
        return new RegisteredCompilation(CompletableFuture.completedFuture(null));
    }
}
