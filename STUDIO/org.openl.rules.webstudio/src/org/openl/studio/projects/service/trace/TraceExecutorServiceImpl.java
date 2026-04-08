package org.openl.studio.projects.service.trace;

import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TreeBuildTracer;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.studio.projects.service.AbstractMethodExecutorService;
import org.openl.studio.projects.service.ExecutionProgressListener;

/**
 * Asynchronous implementation of {@link TraceExecutorService}.
 * <p>
 * Executes trace operations on the {@code testSuiteExecutor} thread pool to avoid
 * blocking HTTP request threads. Uses {@link TreeBuildTracer} to collect trace
 * information during rule execution.
 * </p>
 * <p>
 * The implementation handles:
 * <ul>
 *   <li>TestSuiteMethod tracing with optional test range filtering</li>
 *   <li>Regular method tracing with input parameters and runtime context</li>
 *   <li>Progress notification via {@link ExecutionProgressListener}</li>
 *   <li>Trace tree caching via {@link TraceHelper}</li>
 * </ul>
 * </p>
 */
@Validated
@Component
public class TraceExecutorServiceImpl extends AbstractMethodExecutorService implements TraceExecutorService {

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<ITracerObject> traceTestSuite(@NotNull ExecutionProgressListener listener,
                                                          @NotNull ProjectModel projectModel,
                                                          @NotNull IOpenLTable table,
                                                          String testRanges,
                                                          boolean currentOpenedModule,
                                                          @NotNull TraceHelper traceHelper) {
        return executeWithLifecycle(listener, () -> {
            String uri = table.getUri();
            var method = currentOpenedModule
                    ? projectModel.getOpenedModuleMethod(uri)
                    : projectModel.getMethod(uri);

            if (!(method instanceof TestSuiteMethod testSuiteMethod)) {
                throw new IllegalArgumentException("Table is not a test suite method");
            }

            TestSuite testSuite;
            if (testRanges == null) {
                testSuite = new TestSuite(testSuiteMethod);
            } else {
                int[] indices = testSuiteMethod.getIndices(testRanges);
                testSuite = new TestSuite(testSuiteMethod, indices);
            }

            return executeTrace(projectModel, testSuite, currentOpenedModule, traceHelper);
        });
    }

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<ITracerObject> traceMethod(@NotNull ExecutionProgressListener listener,
                                                        @NotNull ProjectModel projectModel,
                                                        @NotNull IOpenLTable table,
                                                        Object[] params,
                                                        IRulesRuntimeContext runtimeContext,
                                                        boolean currentOpenedModule,
                                                        @NotNull TraceHelper traceHelper) {
        return executeWithLifecycle(listener, () -> {
            var method = resolveMethod(projectModel, table, currentOpenedModule, runtimeContext);
            var db = getDb(projectModel, currentOpenedModule);
            var testSuite = new TestSuite(new TestDescription(method, runtimeContext, params, db));
            return executeTrace(projectModel, testSuite, currentOpenedModule, traceHelper);
        });
    }

    private ITracerObject executeTrace(ProjectModel projectModel,
                                       TestSuite testSuite,
                                       boolean currentOpenedModule,
                                       TraceHelper traceHelper) {
        ITracerObject root;
        try {
            // Initialize tracer before calling traceElement (same pattern as RunTestHelper)
            root = TreeBuildTracer.initialize(true); // Use lazy nodes

            // traceElement handles classloader context, CachingArgumentsCloner, and sequential execution
            projectModel.traceElement(testSuite, currentOpenedModule);

            // Cache trace tree for later retrieval
            traceHelper.cacheTraceTree(root);

            return root;
        } finally {
            TreeBuildTracer.destroy();
        }
    }
}
