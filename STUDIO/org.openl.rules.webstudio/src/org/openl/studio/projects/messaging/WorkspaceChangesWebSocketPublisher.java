package org.openl.studio.projects.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectStateChangedEvent;

/**
 * Turns {@link ProjectStateChangedEvent}s into per-user change pings: a workspace-wide one on
 * {@code /user/topic/workspace/changed} for the projects list, and a per-project one on
 * {@code /user/topic/projects/{id}/changed} for an open page of that project. The per-project ping
 * carries the touched files when the event names them, so the page can refresh an open file precisely.
 *
 * Only real changes ping — open, close, save, delete, branch change, and content changes the files
 * watcher observes on the workspace disk. Compile-status transitions do not: a compile changes
 * nothing the pings stand for, and it already streams on the status destinations. Bursts within a
 * moment collapse into one ping per destination, their files merged.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceChangesWebSocketPublisher {

    private final ProjectSocketNotificationService notificationService;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final NotificationDebouncer debouncer;

    @EventListener
    public void onProjectStateChanged(ProjectStateChangedEvent event) {
        var userName = event.userName();
        debouncer.debounce(userName, () -> notificationService.notifyWorkspaceChanged(userName));
        try {
            var projectId = projectIdentifierMapper.map(event.project());
            var key = userName + "|" + projectId.encode();
            // The debouncer merges the files of a burst and drains them atomically with the window.
            debouncer.debounce(key, event.paths(),
                    files -> notificationService.notifyProjectChanged(userName, projectId, files));
        } catch (RuntimeException e) {
            log.warn("Failed to publish a project change ping", e);
        }
    }
}
