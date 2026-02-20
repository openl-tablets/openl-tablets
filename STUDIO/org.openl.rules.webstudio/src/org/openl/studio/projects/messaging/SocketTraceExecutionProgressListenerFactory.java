package org.openl.studio.projects.messaging;

import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.trace.TraceExecutionProgressListener;
import org.openl.studio.projects.service.trace.TraceExecutionStatus;

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
     * Creates a TraceExecutionProgressListener that notifies the user via WebSocket about trace execution progress.
     *
     * @param user      the user to notify
     * @param projectId the project ID
     * @param tableId   the table ID
     * @return a TraceExecutionProgressListener instance
     */
    public TraceExecutionProgressListener create(CommonUser user, ProjectIdModel projectId, String tableId) {
        return new TraceExecutionProgressListener() {
            @Override
            public void onStatusChanged(TraceExecutionStatus status) {
                socketNotificationService.notifyTraceExecutionStatus(user, projectId, tableId, status);
            }

            @Override
            public void onError(String message, Throwable cause) {
                socketNotificationService.notifyTraceExecutionError(user, projectId, tableId, message);
            }
        };
    }
}
