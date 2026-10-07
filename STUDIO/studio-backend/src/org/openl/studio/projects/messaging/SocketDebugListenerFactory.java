package org.openl.studio.projects.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.trace.DebugListener;

/**
 * Creates {@link DebugListener}s that push debug status changes to a user over WebSocket.
 *
 * <p>The status is sent to {@code /topic/projects/{projectId}/tables/{tableId}/trace/status} together
 * with the session id, since sessions of the same user and table share that topic — the id lets the
 * client drop events of a session it does not watch (for example a stale one reaped in the background).
 * The client reads the current stack from the REST API when it sees {@code suspended}.
 */
@Component
@RequiredArgsConstructor
public class SocketDebugListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    public DebugListener create(CommonUser user, ProjectIdModel projectId, String tableId, String sessionId) {
        return status -> socketNotificationService.notifyTraceDebugStatus(user, projectId, tableId,
                status.getCode(), sessionId);
    }
}
