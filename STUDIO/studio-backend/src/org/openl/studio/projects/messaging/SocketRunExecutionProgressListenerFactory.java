package org.openl.studio.projects.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.ExecutionStatus;

/**
 * Factory for creating run execution progress listeners that notify users via WebSocket.
 */
@RequiredArgsConstructor
@Component
public class SocketRunExecutionProgressListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    /**
     * Creates an ExecutionProgressListener that notifies the user via WebSocket about run execution progress.
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
                socketNotificationService.notifyRunExecutionStatus(user, projectId, tableId, status);
            }

            @Override
            public void onError(String message, Throwable cause) {
                socketNotificationService.notifyRunExecutionError(user, projectId, tableId, message);
            }
        };
    }
}
