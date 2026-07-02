package org.openl.studio.projects.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Creates {@link DebugListener}s that push debug status changes to a user over WebSocket.
 *
 * <p>The status name is sent to {@code /topic/projects/{projectId}/tables/{tableId}/trace/status}; the
 * client reads the current stack from the REST API when it sees {@code SUSPENDED}.
 */
@Component
@RequiredArgsConstructor
public class SocketDebugListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    public DebugListener create(CommonUser user, ProjectIdModel projectId, String tableId) {
        return status -> socketNotificationService.notifyTraceDebugStatus(user, projectId, tableId, status.name());
    }
}
