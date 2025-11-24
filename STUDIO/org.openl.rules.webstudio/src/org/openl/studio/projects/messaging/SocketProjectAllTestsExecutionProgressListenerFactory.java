package org.openl.studio.projects.messaging;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.tests.TestCaseExecutionResult;
import org.openl.studio.projects.service.tests.ProjectTestsExecutionProgressListener;
import org.openl.studio.projects.service.tests.TestExecutionStatus;

@Component
public class SocketProjectAllTestsExecutionProgressListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    public SocketProjectAllTestsExecutionProgressListenerFactory(ProjectSocketNotificationService socketNotificationService) {
        this.socketNotificationService = socketNotificationService;
    }

    /**
     * Creates a ProjectTestsExecutionProgressListener that notifies the user via WebSocket about test execution progress.
     *
     * @param user the user to notify
     * @param projectId the project ID
     * @param mapper a function to map {@link TestUnitsResults} to {@link TestCaseExecutionResult}
     * @return a ProjectTestsExecutionProgressListener instance
     */
    public ProjectTestsExecutionProgressListener create(CommonUser user, ProjectIdModel projectId, Function<TestUnitsResults, TestCaseExecutionResult> mapper) {
        return new ProjectTestsExecutionProgressListener() {
            @Override
            public void onStatusChanged(TestExecutionStatus status) {
                socketNotificationService.notifyProjectTestsExecutionStatus(user, projectId, status);
            }

            @Override
            public void onTestUnitExecuted(TestUnitsResults testUnitsResults) {
                TestCaseExecutionResult result = mapper.apply(testUnitsResults);
                socketNotificationService.notifyProjectTestsExecutionResults(user, projectId, result);
            }
        };
    }

    /**
     * Creates a ProjectTestsExecutionProgressListener that notifies the user via WebSocket about test execution progress for a specific table.
     *
     * @param user the user to notify
     * @param projectId the project ID
     * @param tableId the table ID
     * @param mapper a function to map {@link TestUnitsResults} to {@link TestCaseExecutionResult}
     * @return a ProjectTestsExecutionProgressListener instance
     */
    public ProjectTestsExecutionProgressListener create(CommonUser user, ProjectIdModel projectId, String tableId, Function<TestUnitsResults, TestCaseExecutionResult> mapper) {
        return new ProjectTestsExecutionProgressListener() {
            @Override
            public void onStatusChanged(TestExecutionStatus status) {
                socketNotificationService.notifyProjectTestsExecutionStatus(user, projectId, tableId, status);
            }

            @Override
            public void onTestUnitExecuted(TestUnitsResults testUnitsResults) {
                TestCaseExecutionResult result = mapper.apply(testUnitsResults);
                socketNotificationService.notifyProjectTestsExecutionResults(user, projectId, tableId, result);
            }
        };
    }
}
