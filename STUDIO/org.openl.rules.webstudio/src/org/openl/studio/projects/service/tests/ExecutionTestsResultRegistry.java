package org.openl.studio.projects.service.tests;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.AbstractExecutionResultRegistry;

/**
 * Session-scoped registry for managing test execution tasks.
 * <p>
 * This registry holds at most one test execution task per user session.
 * When a new test run is started, any previously running task is automatically
 * cancelled to prevent resource exhaustion.
 * </p>
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionTestsResultRegistry extends AbstractExecutionResultRegistry<List<TestUnitsResults>> {

    /**
     * Register a new task; cancels previous one if running.
     *
     * @param projectId the project identifier
     * @param task      the test execution future
     */
    public void setTask(ProjectIdModel projectId, CompletableFuture<List<TestUnitsResults>> task) {
        registerTask(projectId, null, task);
    }
}
