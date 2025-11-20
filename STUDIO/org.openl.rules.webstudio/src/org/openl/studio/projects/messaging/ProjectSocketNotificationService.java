package org.openl.studio.projects.messaging;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.tests.TestCaseExecutionResult;
import org.openl.studio.projects.service.tests.TestExecutionStatus;

@Component
@ParametersAreNonnullByDefault
public class ProjectSocketNotificationService {

    private static final  String STATUS = "/status";
    private static final  String RESULTS = "/units";
    private static final String TOPIC_PROJECTS_TESTS = "/topic/projects/%s/tests";
    private static final String TOPIC_PROJECTS_TABLES_TESTS = "/topic/projects/%s/tables/%s/tests";

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

    private String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8);
    }
}
