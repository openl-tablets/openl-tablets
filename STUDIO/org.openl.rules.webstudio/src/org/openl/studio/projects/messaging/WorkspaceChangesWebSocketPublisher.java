package org.openl.studio.projects.messaging;

import java.util.Set;

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
 * moment collapse into one ping per destination, their files and their origins merged.
 *
 * Every ping names the clients whose requests caused it, so the tab that made the change recognises
 * its own echo instead of re-reading the workspace a second time.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceChangesWebSocketPublisher {

    private final ProjectSocketNotificationService notificationService;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final NotificationDebouncer debouncer;
    private final ChangeOriginResolver changeOrigin;

    @EventListener
    public void onProjectStateChanged(ProjectStateChangedEvent event) {
        var userName = event.userName();
        // Read on the publishing thread: the event travels synchronously, so a change made by a
        // request is still on that request's thread here. A change made off one names no origin.
        var origin = changeOrigin.current();
        debouncer.debounce(userName, ChangeNotes.of(Set.of(), origin),
                notes -> notificationService.notifyWorkspaceChanged(userName, notes));
        try {
            var projectId = projectIdentifierMapper.map(event.project());
            var key = userName + "|" + projectId.encode();
            // The debouncer merges the notes of a burst and drains them atomically with the window.
            debouncer.debounce(key, ChangeNotes.of(event.paths(), origin),
                    notes -> notificationService.notifyProjectChanged(userName, projectId, notes));
        } catch (RuntimeException e) {
            log.warn("Failed to publish a project change ping", e);
        }
    }
}
