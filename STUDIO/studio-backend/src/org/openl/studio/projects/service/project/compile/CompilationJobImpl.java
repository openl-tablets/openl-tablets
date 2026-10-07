package org.openl.studio.projects.service.project.compile;

import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RegisteredCompilation;

/**
 * Default {@link CompilationJob} implementation backed by a
 * {@link RegisteredCompilation} registered on the {@link ProjectModel}. The cycle's
 * future is owned by the project model; this class only attaches a logging hook for
 * compilation failures and exposes the wrapped future to consumers.
 *
 * @author Vladyslav Pikus
 */
@Slf4j
@Accessors(fluent = true)
class CompilationJobImpl implements CompilationJob {

    @Getter
    private final ProjectModel project;
    @Accessors(fluent = false)
    private final RegisteredCompilation cycle;
    @Getter
    private final CompletableFuture<Void> future;

    CompilationJobImpl(ProjectModel model) {
        this.project = model;
        this.cycle = model.getCurrentCompilation();
        this.future = cycle.future().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                log.warn("Project compilation failed", throwable);
            }
        });
    }

    boolean isFinished() {
        return future.isDone();
    }

    /**
     * @return {@code true} when this job still observes the project model's current
     *         compile cycle; {@code false} once {@link ProjectModel} has registered a
     *         newer cycle and the cached job's result is no longer authoritative.
     */
    boolean tracksCurrentCompilation() {
        return project.getCurrentCompilation() == cycle;
    }
}
