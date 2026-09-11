package org.openl.studio.projects.service.benchmark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.AbstractExecutionResultRegistry;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Session-scoped registry of the benchmarks a user has taken.
 * <p>
 * At most one benchmark is kept at a time: starting another one abandons the one before it, and what an
 * abandoned benchmark measured is dropped when it ends. The measurements of every benchmark that ended are
 * kept, the newest first, so that they can be compared with each other.
 * </p>
 * <p>
 * The measurements belong to the project they were taken in, because a measurement is read together with the
 * table it was taken on. Another project has none of its own until a benchmark is taken there, which starts a
 * new list.
 * </p>
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionBenchmarkResultRegistry extends AbstractExecutionResultRegistry<List<BenchmarkMeasurement>> {

    private final List<BenchmarkMeasurement> measurements = new ArrayList<>();
    private @Nullable ProjectIdModel measuredProject;
    private @Nullable CompletableFuture<List<BenchmarkMeasurement>> pending;

    /**
     * Register a new benchmark task; abandons the previous one if it is still running.
     *
     * @param projectId the project identifier
     * @param tableId   the table identifier
     * @param task      the benchmark execution future
     */
    public synchronized void setTask(ProjectIdModel projectId,
                                     String tableId,
                                     CompletableFuture<List<BenchmarkMeasurement>> task) {
        Objects.requireNonNull(tableId, "tableId");
        if (projectId.equals(measuredProject)) {
            // A benchmark that ended without being read keeps what it measured: the window compares every
            // measurement taken in the session, and this one is one of them.
            harvest(projectId);
        } else {
            measurements.clear();
            measuredProject = projectId;
        }
        pending = task;
        registerTask(projectId, tableId, task);
    }

    /**
     * The measurements taken in this session for the given project, the newest first.
     *
     * <p>A benchmark that has ended joins them the first time they are read, so that reading them again
     * reports the same rows. One that was abandoned adds nothing, and one that failed says why instead of
     * answering with the rows of the benchmarks before it.
     *
     * <p>A project that was never measured has no measurements, and reading it leaves the benchmark of
     * another project waiting to be read there.
     *
     * @param projectId the project identifier
     * @return the measurements of the project
     * @throws RuntimeException the reason the benchmark that has just ended failed
     */
    public synchronized List<BenchmarkMeasurement> collect(ProjectIdModel projectId) {
        if (!projectId.equals(measuredProject)) {
            return List.of();
        }
        var failure = harvest(projectId);
        if (failure != null) {
            // A benchmark that could not be taken says why. The measurements of the ones before it are not
            // its result, and are there to be read again.
            throw RuntimeExceptionWrapper.wrap(failure);
        }
        return List.copyOf(measurements);
    }

    /**
     * Takes in what the benchmark that has ended measured.
     *
     * <p>A benchmark that is still going on is left alone. One that was abandoned measured nothing.
     *
     * @param projectId the project identifier
     * @return the reason the benchmark failed, or {@code null} when it did not
     */
    private @Nullable Throwable harvest(ProjectIdModel projectId) {
        var task = pending;
        if (task == null || !task.isDone()) {
            return null;
        }
        pending = null;
        if (task.isCancelled()) {
            return null;
        }
        if (task.isCompletedExceptionally()) {
            return task.exceptionNow();
        }
        var taken = getResultIfDone(projectId);
        if (taken != null) {
            measurements.addAll(0, taken);
        }
        return null;
    }

    /**
     * Forget measurements of a project: the ones the identifiers name, or every one of them when no
     * identifier is given.
     *
     * <p>Forgetting all of them also abandons the benchmark that is still running there. Measurements of
     * another project are left alone.
     *
     * @param projectId the project identifier
     * @param ids       identifiers of the measurements to forget, empty for all of them
     */
    public synchronized void forget(ProjectIdModel projectId, Collection<String> ids) {
        if (!projectId.equals(measuredProject)) {
            return;
        }
        if (ids.isEmpty()) {
            clear();
        } else {
            measurements.removeIf(measurement -> ids.contains(measurement.id()));
        }
    }

    /**
     * Forget every measurement, abandoning the benchmark that is still running.
     */
    @Override
    public synchronized void clear() {
        super.clear();
        pending = null;
        measurements.clear();
        measuredProject = null;
    }
}
