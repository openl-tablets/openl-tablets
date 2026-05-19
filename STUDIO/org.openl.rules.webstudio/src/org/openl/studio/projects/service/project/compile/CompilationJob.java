package org.openl.studio.projects.service.project.compile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous compilation handle for a single project/module.
 *
 * <p>The job is created when a caller opens a project; it tracks the underlying
 * compilation process driven by the WebStudio session and lets callers either
 * inspect the live state or await completion via {@link #future()}.
 *
 * @author Vladyslav Pikus
 */
public interface CompilationJob {

    /**
     * Unique identifier of this compilation job.
     */
    UUID id();

    /**
     * Current lifecycle state of the job.
     */
    CompilationStatus status();

    /**
     * Compilation progress as a percentage in the range {@code [0, 100]}.
     */
    int progress();

    /**
     * Result of the compilation if it has completed successfully.
     */
    Optional<CompilationResult> result();

    /**
     * Error that caused the compilation to fail, if any.
     */
    Optional<Throwable> error();

    /**
     * Future that completes once the compilation finishes (successfully or
     * exceptionally). Calling {@code future().join()} blocks the calling thread
     * until the job terminates.
     */
    CompletableFuture<CompilationResult> future();
}
