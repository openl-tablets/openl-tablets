package org.openl.studio.projects.messaging;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.model.tests.TestCaseExecutionResult;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.tests.TestExecutionStatus;

@Component
@ParametersAreNonnullByDefault
public class ProjectSocketNotificationService {

    private static final  String STATUS = "/status";
    private static final  String RESULTS = "/units";
    private static final String TOPIC_PROJECTS_TESTS = "/topic/projects/%s/tests";
    private static final String TOPIC_PROJECTS_TABLES_TESTS = "/topic/projects/%s/tables/%s/tests";
    private static final String TOPIC_PROJECTS_TABLES_TRACE = "/topic/projects/%s/tables/%s/trace";
    private static final String TOPIC_PROJECTS_TABLES_RUN = "/topic/projects/%s/tables/%s/run";
    private static final String TOPIC_PROJECTS_STATUS = "/topic/projects/%s/status";
    private static final String TOPIC_PROJECTS_BRANCHES_STATUS = "/topic/projects/%s/branches/%s/status";

    private final SimpMessagingTemplate messagingTemplate;

    public ProjectSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Notifies user about test execution status change.
     *
     * @param user user to notify
     * @param projectId project id
     * @param status new test execution status
     */
    public void notifyProjectTestsExecutionStatus(CommonUser user, ProjectIdModel projectId, TestExecutionStatus status) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TESTS.formatted(encodePathSegment(projectId.encode())) + STATUS,
                status.name());
    }

    /**
     * Notifies user about test execution status change for a specific table.
     *
     * @param user user
     * @param projectId projectId
     * @param tableId tableId
     * @param status status
     */
    public void notifyProjectTestsExecutionStatus(CommonUser user, ProjectIdModel projectId, String tableId, TestExecutionStatus status) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TABLES_TESTS.formatted(encodePathSegment(projectId.encode()), encodePathSegment(tableId)) + STATUS,
                status.name());
    }

    /**
     * Notifies user about executed test case result.
     *
     * @param user user to notify
     * @param projectId project id
     * @param testCaseExecutionResult executed test case result
     */
    public void notifyProjectTestsExecutionResults(CommonUser user, ProjectIdModel projectId, TestCaseExecutionResult testCaseExecutionResult) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TESTS.formatted(encodePathSegment(projectId.encode())) + RESULTS,
                testCaseExecutionResult);
    }

    /**
     * Notifies user about executed test case result for a specific table.
     *
     * @param user user
     * @param projectId projectId
     * @param tableId tableId
     * @param testCaseExecutionResult testCaseExecutionResult
     */
    public void notifyProjectTestsExecutionResults(CommonUser user, ProjectIdModel projectId, String tableId, TestCaseExecutionResult testCaseExecutionResult) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TABLES_TESTS.formatted(encodePathSegment(projectId.encode()), encodePathSegment(tableId)) + RESULTS,
                testCaseExecutionResult);
    }

    /**
     * Notifies user about trace execution status change for a specific table.
     *
     * @param user      user to notify
     * @param projectId project id
     * @param tableId   table id
     * @param status    new trace execution status
     */
    public void notifyTraceExecutionStatus(CommonUser user, ProjectIdModel projectId, String tableId, ExecutionStatus status) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TABLES_TRACE.formatted(encodePathSegment(projectId.encode()), encodePathSegment(tableId)) + STATUS,
                status.name());
    }

    /**
     * Notifies user about trace execution error for a specific table.
     *
     * @param user         user to notify
     * @param projectId    project id
     * @param tableId      table id
     * @param errorMessage error message
     */
    public void notifyTraceExecutionError(CommonUser user, ProjectIdModel projectId, String tableId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TABLES_TRACE.formatted(encodePathSegment(projectId.encode()), encodePathSegment(tableId)) + STATUS,
                java.util.Map.of("status", "ERROR", "message", errorMessage));
    }

    /**
     * Notifies user about run execution status change for a specific table.
     *
     * @param user      user to notify
     * @param projectId project id
     * @param tableId   table id
     * @param status    new run execution status
     */
    public void notifyRunExecutionStatus(CommonUser user, ProjectIdModel projectId, String tableId, ExecutionStatus status) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TABLES_RUN.formatted(encodePathSegment(projectId.encode()), encodePathSegment(tableId)) + STATUS,
                status.name());
    }

    /**
     * Notifies user about run execution error for a specific table.
     *
     * @param user         user to notify
     * @param projectId    project id
     * @param tableId      table id
     * @param errorMessage error message
     */
    public void notifyRunExecutionError(CommonUser user, ProjectIdModel projectId, String tableId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(user.getUserName(),
                TOPIC_PROJECTS_TABLES_RUN.formatted(encodePathSegment(projectId.encode()), encodePathSegment(tableId)) + STATUS,
                java.util.Map.of("status", "ERROR", "message", errorMessage));
    }

    /**
     * Notifies user about a project status change by sending the full
     * {@link org.openl.studio.projects.model.project.status.ProjectStatusViewModel}.
     *
     * <p>Destination depends on whether the project supports branches:
     * <ul>
     *   <li>branch present → {@code /topic/projects/{projectId}/branches/{branch}/status}</li>
     *   <li>branch absent  → {@code /topic/projects/{projectId}/status}</li>
     * </ul>
     *
     * @param userName  destination user (captured from {@code SecurityContext} at the
     *                  call site so async callbacks can still target the right user)
     * @param projectId target project identifier
     * @param branch    branch name; {@code null}/blank for repositories without branch
     *                  support (the topic then omits the {@code /branches/...} segment)
     * @param status    full status view to push
     */
    public void notifyProjectStatus(String userName,
                                    ProjectIdModel projectId,
                                    String branch,
                                    ProjectStatusViewModel status) {
        var encodedProjectId = encodePathSegment(projectId.encode());
        var destination = branch == null || branch.isBlank()
                ? TOPIC_PROJECTS_STATUS.formatted(encodedProjectId)
                : TOPIC_PROJECTS_BRANCHES_STATUS.formatted(encodedProjectId, encodePathSegment(branch));
        messagingTemplate.convertAndSendToUser(userName, destination, status);
    }

    private String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8);
    }
}
