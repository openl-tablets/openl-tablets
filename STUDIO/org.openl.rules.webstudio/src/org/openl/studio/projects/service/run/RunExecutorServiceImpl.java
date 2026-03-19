package org.openl.studio.projects.service.run;

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
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Asynchronous implementation of {@link RunExecutorService}.
 * <p>
 * Executes run operations on the {@code testSuiteExecutor} thread pool to avoid
 * blocking HTTP request threads.
 * </p>
 */
@Validated
@Component
public class RunExecutorServiceImpl implements RunExecutorService {

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<TestUnitsResults> runMethod(@NotNull RunExecutionProgressListener listener,
                                                         @NotNull ProjectModel projectModel,
                                                         @NotNull IOpenLTable table,
                                                         Object[] params,
                                                         IRulesRuntimeContext runtimeContext,
                                                         boolean currentOpenedModule) {
        listener.onStatusChanged(RunExecutionStatus.STARTED);

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

            TestUnitsResults result = projectModel.runTest(testSuite, currentOpenedModule);

            if (Thread.currentThread().isInterrupted()) {
                listener.onStatusChanged(RunExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(result);
            }

            listener.onStatusChanged(RunExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            if (isInterruptedException(e)) {
                listener.onStatusChanged(RunExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(null);
            }
            listener.onError(e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
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
