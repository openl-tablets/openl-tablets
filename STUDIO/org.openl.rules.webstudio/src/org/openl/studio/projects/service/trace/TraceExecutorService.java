package org.openl.studio.projects.service.trace;

import java.util.concurrent.CompletableFuture;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;

/**
 * Service for executing rule tracing asynchronously.
 * <p>
 * This service provides methods for tracing both test suite methods and
 * regular executable methods. Trace execution is performed asynchronously
 * on a dedicated thread pool to prevent blocking the request thread.
 * </p>
 * <p>
 * The trace results are captured in a {@link TraceHelper} which caches
 * the trace tree for subsequent lazy node retrieval.
 * </p>
 */
public interface TraceExecutorService {

    /**
     * Execute trace for a TestSuiteMethod with optional test ranges.
     *
     * @param listener            progress listener
     * @param projectModel        project model
     * @param table               the test suite table
     * @param testRanges          test ranges (e.g., "1-3,5") or null for all
     * @param currentOpenedModule if true, use currently opened module; otherwise, use full project
     * @param traceHelper         trace helper for caching results
     * @return a future that completes with the root trace object
     */
    CompletableFuture<ITracerObject> traceTestSuite(TraceExecutionProgressListener listener,
                                                    ProjectModel projectModel,
                                                    IOpenLTable table,
                                                    String testRanges,
                                                    boolean currentOpenedModule,
                                                    TraceHelper traceHelper);

    /**
     * Execute trace for any executable method with input parameters.
     *
     * @param listener            progress listener
     * @param projectModel        project model
     * @param table               the table to trace
     * @param params              input parameters
     * @param runtimeContext      runtime context (may be null)
     * @param currentOpenedModule if true, use currently opened module; otherwise, use full project
     * @param traceHelper         trace helper for caching results
     * @return a future that completes with the root trace object
     */
    CompletableFuture<ITracerObject> traceMethod(TraceExecutionProgressListener listener,
                                                 ProjectModel projectModel,
                                                 IOpenLTable table,
                                                 Object[] params,
                                                 IRulesRuntimeContext runtimeContext,
                                                 boolean currentOpenedModule,
                                                 TraceHelper traceHelper);
}
