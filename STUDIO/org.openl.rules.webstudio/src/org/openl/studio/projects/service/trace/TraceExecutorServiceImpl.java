package org.openl.studio.projects.service.trace;

import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import org.openl.CompiledOpenClass;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TreeBuildTracer;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

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
 *   <li>Progress notification via {@link TraceExecutionProgressListener}</li>
 *   <li>Trace tree caching via {@link TraceHelper}</li>
 * </ul>
 * </p>
 */
@Validated
@Component
public class TraceExecutorServiceImpl implements TraceExecutorService {

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<ITracerObject> traceTestSuite(@NotNull TraceExecutionProgressListener listener,
                                                          @NotNull ProjectModel projectModel,
                                                          @NotNull IOpenLTable table,
                                                          String testRanges,
                                                          boolean currentOpenedModule,
                                                          @NotNull TraceHelper traceHelper) {
        listener.onStatusChanged(TraceExecutionStatus.STARTED);

        try {
            String uri = table.getUri();
            IOpenMethod method = currentOpenedModule
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

            ITracerObject root = executeTrace(projectModel, testSuite, currentOpenedModule, traceHelper);

            // Check if thread was interrupted during execution
            if (Thread.currentThread().isInterrupted()) {
                listener.onStatusChanged(TraceExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(root);
            }

            listener.onStatusChanged(TraceExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(root);
        } catch (Exception e) {
            if (isInterruptedException(e)) {
                listener.onStatusChanged(TraceExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(null);
            }
            listener.onError(e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<ITracerObject> traceMethod(@NotNull TraceExecutionProgressListener listener,
                                                        @NotNull ProjectModel projectModel,
                                                        @NotNull IOpenLTable table,
                                                        Object[] params,
                                                        IRulesRuntimeContext runtimeContext,
                                                        boolean currentOpenedModule,
                                                        @NotNull TraceHelper traceHelper) {
        listener.onStatusChanged(TraceExecutionStatus.STARTED);

        try {
            String uri = table.getUri();
            IOpenMethod method = currentOpenedModule
                    ? projectModel.getOpenedModuleMethod(uri)
                    : projectModel.getMethod(uri);

            if (method instanceof OpenMethodDispatcher) {
                method = projectModel.getCurrentDispatcherMethod(method, uri);
                if (method == null) {
                    throw new IllegalStateException("Failed to resolve dispatcher method for table: " + uri);
                }
            }

            // If runtime context is provided, get project-level method
            if (runtimeContext != null) {
                CompiledOpenClass compiledOpenClass = currentOpenedModule
                        ? projectModel.getOpenedModuleCompiledOpenClass()
                        : projectModel.getCompiledOpenClass();
                method = compiledOpenClass.getOpenClassWithErrors()
                        .getMethod(method.getName(), method.getSignature().getParameterTypes());
            }

            IDataBase db = getDb(projectModel, currentOpenedModule);
            TestSuite testSuite = new TestSuite(new TestDescription(method, runtimeContext, params, db));

            ITracerObject root = executeTrace(projectModel, testSuite, currentOpenedModule, traceHelper);

            // Check if thread was interrupted during execution
            if (Thread.currentThread().isInterrupted()) {
                listener.onStatusChanged(TraceExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(root);
            }

            listener.onStatusChanged(TraceExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(root);

        } catch (Exception e) {
            if (isInterruptedException(e)) {
                listener.onStatusChanged(TraceExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(null);
            }
            listener.onError(e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
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

    private static IDataBase getDb(ProjectModel projectModel, boolean currentOpenedModule) {
        if (projectModel == null) {
            return null;
        }
        CompiledOpenClass compiledOpenClass = currentOpenedModule
                ? projectModel.getOpenedModuleCompiledOpenClass()
                : projectModel.getCompiledOpenClass();
        IOpenClass moduleClass = compiledOpenClass.getOpenClassWithErrors();
        if (moduleClass instanceof XlsModuleOpenClass xlsModuleOpenClass) {
            return xlsModuleOpenClass.getDataBase();
        }
        return null;
    }

    /**
     * Check if the exception is caused by thread interruption.
     */
    private static boolean isInterruptedException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof InterruptedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
