package org.openl.studio.projects.service.project.compile;

import java.util.concurrent.CompletableFuture;

import org.openl.rules.ui.ProjectModel;

/**
 * Asynchronous compilation handle for a single project/module.
 *
 * <p>The job is created when a caller opens a project; it wraps the underlying
 * compilation cycle driven by the WebStudio session and lets callers wait for
 * completion via {@link #future()} or inspect the live model state via
 * {@link #project()}.
 *
 * @author Vladyslav Pikus
 */
public interface CompilationJob {

    /**
     * Future that completes once the compilation finishes (successfully or
     * exceptionally). Calling {@code future().join()} blocks the calling thread
     * until the job terminates.
     */
    CompletableFuture<Void> future();

    /**
     * Project model whose compilation this job tracks. Always non-null; the
     * returned model becomes fully compiled once {@link #future()} completes
     * successfully.
     */
    ProjectModel project();
}
