package org.openl.studio.projects.service.run;

import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.ExecutionProgressListener;

/**
 * Service for executing regular (non-test) methods asynchronously.
 * <p>
 * Execution is performed asynchronously on a dedicated thread pool
 * to prevent blocking the request thread.
 * </p>
 * <p>
 * For test table execution, use the Tests API ({@code TestsExecutorService}) instead.
 * </p>
 */
public interface RunExecutorService {

    /**
     * Execute any executable method with input parameters.
     *
     * @param listener            progress listener (must not be null)
     * @param projectModel        project model (must not be null)
     * @param table               the table to run (must not be null)
     * @param params              input parameters (may be null)
     * @param runtimeContext      runtime context (may be null)
     * @param currentOpenedModule if true, use currently opened module; otherwise, use full project
     * @return a future that completes with the test results
     */
    CompletableFuture<TestUnitsResults> runMethod(@NotNull ExecutionProgressListener listener,
                                                  @NotNull ProjectModel projectModel,
                                                  @NotNull IOpenLTable table,
                                                  Object[] params,
                                                  IRulesRuntimeContext runtimeContext,
                                                  boolean currentOpenedModule);
}
