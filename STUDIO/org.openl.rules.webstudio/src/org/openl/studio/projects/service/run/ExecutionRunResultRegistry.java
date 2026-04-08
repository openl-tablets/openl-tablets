package org.openl.studio.projects.service.run;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.AbstractExecutionResultRegistry;

/**
 * Session-scoped registry for managing run execution tasks.
 * <p>
 * This registry holds at most one run execution task per user session.
 * When a new run is started, any previously running task is automatically
 * cancelled to prevent resource exhaustion.
 * </p>
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionRunResultRegistry extends AbstractExecutionResultRegistry<TestUnitsResults> {

    /**
     * Register a new run task; cancels previous one if running.
     *
     * @param projectId the project identifier
     * @param tableId   the table identifier
     * @param task      the run execution future
     */
    public void setTask(ProjectIdModel projectId,
                        String tableId,
                        CompletableFuture<TestUnitsResults> task) {
        Objects.requireNonNull(tableId, "tableId");
        registerTask(projectId, tableId, task);
    }
}
