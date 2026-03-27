package org.openl.studio.projects.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.tests.TestCaseExecutionResult;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.tests.TestExecutionStatus;

@ExtendWith(MockitoExtension.class)
class ProjectSocketNotificationServiceTest {

    private static final String USER_NAME = "testUser";
    private static final String REPO_ID = "design";
    private static final String PROJECT_NAME = "MyProject";
    private static final String TABLE_ID = "table/with special+chars";

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CommonUser user;

    private ProjectSocketNotificationService service;
    private ProjectIdModel projectId;

    @BeforeEach
    void setUp() {
        when(user.getUserName()).thenReturn(USER_NAME);
        service = new ProjectSocketNotificationService(messagingTemplate);
        projectId = ProjectIdModel.builder()
                .repository(REPO_ID)
                .projectName(PROJECT_NAME)
                .build();
    }

    @Test
    void notifyProjectTestsExecutionStatus() {
        service.notifyProjectTestsExecutionStatus(user, projectId, TestExecutionStatus.STARTED);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tests/status".formatted(encodedProjectId()),
                "STARTED");
    }

    @Test
    void notifyProjectTestsExecutionStatus_completed() {
        service.notifyProjectTestsExecutionStatus(user, projectId, TestExecutionStatus.COMPLETED);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tests/status".formatted(encodedProjectId()),
                "COMPLETED");
    }

    @Test
    void notifyProjectTableTestsExecutionStatus() {
        service.notifyProjectTestsExecutionStatus(user, projectId, TABLE_ID, TestExecutionStatus.PENDING);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/tests/status".formatted(encodedProjectId(), encodedTableId()),
                "PENDING");
    }

    @Test
    void notifyProjectTestsExecutionResults() {
        var result = TestCaseExecutionResult.builder()
                .name("testCase1")
                .tableId("tbl1")
                .executionTimeMs(42.5)
                .numberOfTests(10)
                .numberOfFailures(2)
                .build();

        service.notifyProjectTestsExecutionResults(user, projectId, result);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tests/units".formatted(encodedProjectId()),
                result);
    }

    @Test
    void notifyProjectTableTestsExecutionResults() {
        var result = TestCaseExecutionResult.builder()
                .name("testCase2")
                .tableId("tbl2")
                .executionTimeMs(10.0)
                .numberOfTests(5)
                .numberOfFailures(0)
                .build();

        service.notifyProjectTestsExecutionResults(user, projectId, TABLE_ID, result);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/tests/units".formatted(encodedProjectId(), encodedTableId()),
                result);
    }

    @Test
    void notifyTraceExecutionStatus() {
        service.notifyTraceExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.STARTED);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/trace/status".formatted(encodedProjectId(), encodedTableId()),
                "STARTED");
    }

    @Test
    void notifyTraceExecutionStatus_completed() {
        service.notifyTraceExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.COMPLETED);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/trace/status".formatted(encodedProjectId(), encodedTableId()),
                "COMPLETED");
    }

    @Test
    void notifyTraceExecutionError() {
        service.notifyTraceExecutionError(user, projectId, TABLE_ID, "Something went wrong");

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/trace/status".formatted(encodedProjectId(), encodedTableId()),
                Map.of("status", "ERROR", "message", "Something went wrong"));
    }

    @Test
    void notifyRunExecutionStatus() {
        service.notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.PENDING);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/run/status".formatted(encodedProjectId(), encodedTableId()),
                "PENDING");
    }

    @Test
    void notifyRunExecutionStatus_completed() {
        service.notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.COMPLETED);

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/run/status".formatted(encodedProjectId(), encodedTableId()),
                "COMPLETED");
    }

    @Test
    void notifyRunExecutionError() {
        service.notifyRunExecutionError(user, projectId, TABLE_ID, "Execution failed");

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/tables/%s/run/status".formatted(encodedProjectId(), encodedTableId()),
                Map.of("status", "ERROR", "message", "Execution failed"));
    }

    private String encodedProjectId() {
        return URLEncoder.encode(projectId.encode(), StandardCharsets.UTF_8);
    }

    private String encodedTableId() {
        return URLEncoder.encode(TABLE_ID, StandardCharsets.UTF_8);
    }
}
