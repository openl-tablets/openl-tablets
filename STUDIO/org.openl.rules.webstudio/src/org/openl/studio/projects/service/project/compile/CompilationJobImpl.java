package org.openl.studio.projects.service.project.compile;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RegisteredCompilation;

/**
 * Default {@link CompilationJob} implementation backed by a
 * {@link RegisteredCompilation} registered on the {@link ProjectModel}. The cycle's
 * future is owned by the project model and completes when the underlying asynchronous
 * compilation finishes, so this class never polls — it only adapts the cycle future
 * into the {@link CompilationResult}-typed future required by {@link CompilationJob}.
 *
 * @author Vladyslav Pikus
 */
@Slf4j
@Accessors(fluent = true)
class CompilationJobImpl implements CompilationJob {

    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final ProjectModel project;
    @Accessors(fluent = false)
    private final RegisteredCompilation cycle;
    @Getter
    private final CompletableFuture<CompilationResult> future;

    CompilationJobImpl(ProjectModel model) {
        this.project = model;
        this.cycle = model.getCurrentCompilation();
        // The compile start time is owned by the cycle itself (recorded when ProjectModel
        // registered it), so duration stays accurate even if this job observer is created
        // long after the compile was kicked off (e.g. the JSF Project Tree triggered the
        // compile and the REST status endpoint adopts it later via the registry).
        var cycleStartedAt = cycle.startedAt();
        this.future = cycle.future().handle((ignored, throwable) -> {
            if (throwable != null) {
                log.warn("Project compilation failed", throwable);
                if (throwable instanceof RuntimeException re) {
                    throw re;
                }
                throw new CompletionExceptionWrapper(throwable);
            }
            var compilationStatus = model.getCompilationStatus();
            return new CompilationResult(
                    id,
                    Duration.between(cycleStartedAt, Instant.now()),
                    compilationStatus.getModulesCompiled(),
                    compilationStatus.getModulesCount());
        });
    }

    @Override
    public CompilationStatus status() {
        if (!future.isDone()) {
            return CompilationStatus.RUNNING;
        }
        return future.isCompletedExceptionally() ? CompilationStatus.FAILED : CompilationStatus.SUCCEEDED;
    }

    @Override
    public int progress() {
        var compilationStatus = project.getCompilationStatus();
        var total = compilationStatus.getModulesCount();
        if (total <= 0) {
            return status() == CompilationStatus.SUCCEEDED ? 100 : 0;
        }
        var percent = (int) Math.round(100.0 * compilationStatus.getModulesCompiled() / total);
        return Math.clamp(percent, 0, 100);
    }

    @Override
    public Optional<CompilationResult> result() {
        if (status() != CompilationStatus.SUCCEEDED) {
            return Optional.empty();
        }
        return Optional.ofNullable(future.getNow(null));
    }

    @Override
    public Optional<Throwable> error() {
        if (!future.isCompletedExceptionally()) {
            return Optional.empty();
        }
        try {
            future.getNow(null);
            return Optional.empty();
        } catch (Throwable t) {
            var cause = t.getCause();
            return Optional.of(cause != null ? cause : t);
        }
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

    private static final class CompletionExceptionWrapper extends RuntimeException {
        CompletionExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
