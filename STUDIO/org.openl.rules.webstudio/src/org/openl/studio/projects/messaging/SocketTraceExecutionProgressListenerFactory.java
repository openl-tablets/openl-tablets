package org.openl.studio.projects.messaging;

import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.ExecutionStatus;

/**
 * Factory for creating trace execution progress listeners that notify users via WebSocket.
 */
@Component
public class SocketTraceExecutionProgressListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    public SocketTraceExecutionProgressListenerFactory(ProjectSocketNotificationService socketNotificationService) {
        this.socketNotificationService = socketNotificationService;
    }

    /**
     * Creates an ExecutionProgressListener that notifies the user via WebSocket about trace execution progress.
     *
     * @param user      the user to notify
     * @param projectId the project ID
     * @param tableId   the table ID
     * @return an ExecutionProgressListener instance
     */
    public ExecutionProgressListener create(CommonUser user, ProjectIdModel projectId, String tableId) {
        return new ExecutionProgressListener() {
            @Override
            public void onStatusChanged(ExecutionStatus status) {
                socketNotificationService.notifyTraceExecutionStatus(user, projectId, tableId, status);
            }

            @Override
            public void onError(String message, Throwable cause) {
                socketNotificationService.notifyTraceExecutionError(user, projectId, tableId, message);
            }
        };
    }
}
