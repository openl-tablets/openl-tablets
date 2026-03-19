package org.openl.studio.projects.messaging;

import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.run.RunExecutionProgressListener;
import org.openl.studio.projects.service.run.RunExecutionStatus;

/**
 * Factory for creating run execution progress listeners that notify users via WebSocket.
 */
@Component
public class SocketRunExecutionProgressListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    public SocketRunExecutionProgressListenerFactory(ProjectSocketNotificationService socketNotificationService) {
        this.socketNotificationService = socketNotificationService;
    }

    /**
     * Creates a RunExecutionProgressListener that notifies the user via WebSocket about run execution progress.
     *
     * @param user      the user to notify
     * @param projectId the project ID
     * @param tableId   the table ID
     * @return a RunExecutionProgressListener instance
     */
    public RunExecutionProgressListener create(CommonUser user, ProjectIdModel projectId, String tableId) {
        return new RunExecutionProgressListener() {
            @Override
            public void onStatusChanged(RunExecutionStatus status) {
                socketNotificationService.notifyRunExecutionStatus(user, projectId, tableId, status);
            }

            @Override
            public void onError(String message, Throwable cause) {
                socketNotificationService.notifyRunExecutionError(user, projectId, tableId, message);
            }
        };
    }
}
