package org.openl.studio.projects.service;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.openl.CompiledOpenClass;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Abstract base for asynchronous method executor services (run, trace, etc.).
 * <p>
 * Provides shared logic for method resolution, database retrieval,
 * interruption detection, and a lifecycle template that handles
 * progress listener notifications.
 * </p>
 */
public abstract class AbstractMethodExecutorService {

    /**
     * Resolves the executable method from a table, handling {@link OpenMethodDispatcher}
     * and optional runtime context re-resolution at the project level.
     *
     * @param projectModel        project model
     * @param table               the table to resolve
     * @param currentOpenedModule if true, resolve from the currently opened module
     * @param runtimeContext      if non-null, re-resolve the method at the project level
     * @return the resolved method
     * @throws IllegalStateException if a dispatcher method cannot be resolved
     */
    protected static IOpenMethod resolveMethod(ProjectModel projectModel,
                                               IOpenLTable table,
                                               boolean currentOpenedModule,
                                               IRulesRuntimeContext runtimeContext) {
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

        return method;
    }

    /**
     * Retrieves the {@link IDataBase} from the project model's compiled class.
     *
     * @param projectModel        project model
     * @param currentOpenedModule if true, use the currently opened module
     * @return the database, or {@code null} if unavailable
     */
    protected static IDataBase getDb(ProjectModel projectModel, boolean currentOpenedModule) {
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
     * Executes a task within a standard lifecycle: notifies the listener of
     * {@link ExecutionStatus#STARTED STARTED}, then on success
     * {@link ExecutionStatus#COMPLETED COMPLETED} (or {@link ExecutionStatus#INTERRUPTED INTERRUPTED}
     * if the thread was interrupted), and on failure delegates to the listener's error handler.
     *
     * @param listener progress listener
     * @param task     the task to execute
     * @param <T>      result type
     * @return a future that completes with the task result
     */
    protected <T> CompletableFuture<T> executeWithLifecycle(ExecutionProgressListener listener,
                                                            Callable<T> task) {
        listener.onStatusChanged(ExecutionStatus.STARTED);
        try {
            T result = task.call();

            if (Thread.currentThread().isInterrupted()) {
                listener.onStatusChanged(ExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(result);
            }

            listener.onStatusChanged(ExecutionStatus.COMPLETED);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            if (isInterruptedException(e)) {
                listener.onStatusChanged(ExecutionStatus.INTERRUPTED);
                return CompletableFuture.completedFuture(null);
            }
            listener.onError(e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Checks if the exception is caused by thread interruption.
     */
    protected static boolean isInterruptedException(Throwable e) {
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
