package org.openl.studio.projects.messaging;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
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
        // Lenient: the workspace-ping tests never read the user mock.
        lenient().when(user.getUserName()).thenReturn(USER_NAME);
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

    @Test
    void notifyWorkspaceChanged_pings_the_user_only() {
        service.notifyWorkspaceChanged(USER_NAME);

        verify(messagingTemplate).convertAndSendToUser(USER_NAME, "/topic/workspace/changed", "CHANGED");
    }

    @Test
    void notifyProjectsChanged_broadcasts_a_bare_ping() {
        service.notifyProjectsChanged();

        // No project data rides along: every subscriber re-reads the list under its own ACL.
        verify(messagingTemplate).convertAndSend("/topic/projects/changed", "CHANGED");
    }

    @Test
    void notifyWorkspaceProjectStatus_pushes_onto_the_one_stream_of_the_user() {
        var status = mock(ProjectStatusViewModel.class);

        service.notifyWorkspaceProjectStatus(USER_NAME, status);

        // One destination for every project: the status names its own project, the client routes it.
        verify(messagingTemplate).convertAndSendToUser(USER_NAME, "/topic/workspace/projects/status", status);
    }

    @Test
    void notifyProjectChanged_pings_the_project_page_of_the_user_naming_the_files() {
        service.notifyProjectChanged(USER_NAME, projectId, Set.of("rules/B.xlsx", "rules/A.xlsx"));

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/changed".formatted(encodedProjectId()),
                Map.of("files", List.of("rules/A.xlsx", "rules/B.xlsx")));
    }

    @Test
    void notifyProjectChanged_sends_an_empty_file_list_for_a_project_wide_change() {
        service.notifyProjectChanged(USER_NAME, projectId, Set.of());

        verify(messagingTemplate).convertAndSendToUser(
                USER_NAME,
                "/topic/projects/%s/changed".formatted(encodedProjectId()),
                Map.of("files", List.of()));
    }

    private String encodedProjectId() {
        return URLEncoder.encode(projectId.encode(), StandardCharsets.UTF_8);
    }

    private String encodedTableId() {
        return URLEncoder.encode(TABLE_ID, StandardCharsets.UTF_8);
    }
}
