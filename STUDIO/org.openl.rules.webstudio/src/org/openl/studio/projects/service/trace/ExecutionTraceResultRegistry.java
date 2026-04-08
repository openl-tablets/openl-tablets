package org.openl.studio.projects.service.trace;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.AbstractExecutionResultRegistry;

/**
 * Session-scoped registry for managing trace execution tasks.
 * <p>
 * This registry holds at most one trace execution task per user session.
 * When a new trace is started, any previously running trace is automatically
 * cancelled to prevent resource exhaustion.
 * </p>
 * <p>
 * In addition to the execution future, this registry stores the {@link TraceHelper}
 * containing cached trace results for lazy loading.
 * </p>
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionTraceResultRegistry extends AbstractExecutionResultRegistry<ITracerObject> {

    private volatile TraceHelper traceHelper;

    /**
     * Register a new trace task; cancels previous one if running.
     *
     * @param projectId   the project identifier
     * @param tableId     the table identifier
     * @param task        the trace execution future
     * @param traceHelper the trace helper for caching results
     */
    public void setTask(ProjectIdModel projectId,
                        String tableId,
                        CompletableFuture<ITracerObject> task,
                        TraceHelper traceHelper) {
        Objects.requireNonNull(tableId, "tableId");
        Objects.requireNonNull(traceHelper, "traceHelper");
        this.traceHelper = traceHelper;
        registerTask(projectId, tableId, task);
    }

    @Override
    public void clear() {
        this.traceHelper = null;
        super.clear();
    }

    /**
     * Return the trace helper if the task completed successfully.
     * Returns null if the task is not done or failed/cancelled.
     *
     * @param projectId the project identifier
     * @return the trace helper with cached results, or null
     */
    public TraceHelper getTraceHelperIfDone(ProjectIdModel projectId) {
        var result = getResultIfDone(projectId);
        return result != null ? traceHelper : null;
    }
}
