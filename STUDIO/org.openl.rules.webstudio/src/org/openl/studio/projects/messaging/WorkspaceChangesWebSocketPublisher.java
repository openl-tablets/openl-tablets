package org.openl.studio.projects.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.openl.studio.projects.service.ProjectStateChangedEvent;

/**
 * Turns {@link ProjectStateChangedEvent}s into per-user workspace pings on
 * {@code /user/topic/workspace/changed}, so the user's open screens learn that the projects they show
 * may be stale.
 *
 * Only real changes ping — open, close, save, delete, branch change, and content changes the files
 * watcher observes on the workspace disk. Compile-status transitions do not: a compile changes
 * nothing the pings stand for, and it already streams on the status destinations. Bursts within a
 * moment collapse into one ping per user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceChangesWebSocketPublisher {

    private final ProjectSocketNotificationService notificationService;
    private final NotificationDebouncer debouncer;

    @EventListener
    public void onProjectStateChanged(ProjectStateChangedEvent event) {
        notifyWorkspaceChanged(event.userName());
    }

    private void notifyWorkspaceChanged(String userName) {
        debouncer.debounce(userName, () -> notificationService.notifyWorkspaceChanged(userName));
    }
}
