package org.openl.studio.projects.service.benchmark;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.ExecutionProgressListener;

/**
 * Service for measuring how fast a table runs.
 * <p>
 * The measurement is performed asynchronously on a dedicated thread pool
 * to prevent blocking the request thread.
 * </p>
 */
public interface BenchmarkExecutorService {

    /**
     * Measure a table by running it until the measurement lasts long enough to be meaningful.
     * <p>
     * A test table is measured over its test cases: the whole table at once when every case is asked for,
     * and each of the chosen cases on its own otherwise. Any other table is measured over the input it is
     * given, which is a single test case.
     * </p>
     *
     * @param listener            progress listener (must not be null)
     * @param projectModel        project model (must not be null)
     * @param table               the table to measure (must not be null)
     * @param testRanges          test cases of a test table to measure (null for all of them)
     * @param params              input parameters of a table that is not a test table (may be null)
     * @param runtimeContext      runtime context (may be null)
     * @param currentOpenedModule if true, use currently opened module; otherwise, use full project
     * @return a future that completes with the measurements, in the order they were taken
     */
    CompletableFuture<List<BenchmarkMeasurement>> benchmark(@NotNull ExecutionProgressListener listener,
                                                            @NotNull ProjectModel projectModel,
                                                            @NotNull IOpenLTable table,
                                                            @Nullable String testRanges,
                                                            Object @Nullable [] params,
                                                            @Nullable IRulesRuntimeContext runtimeContext,
                                                            boolean currentOpenedModule);
}
