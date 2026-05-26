package org.openl.studio.projects.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.openl.rules.ui.ProjectStatusChangedEvent;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;

/**
 * Listens for {@link ProjectStatusChangedEvent}s published by
 * {@code org.openl.rules.ui.ProjectModel} and pushes the full
 * {@link org.openl.studio.projects.model.project.status.ProjectStatusViewModel} to the
 * {@code /topic/projects/{projectId}[/branches/{branch}]/status} WebSocket destination
 * of the originating user.
 *
 * @author Vladyslav Pikus
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectStatusWebSocketPublisher {

    private final ProjectStatusMapper projectStatusMapper;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final ProjectSocketNotificationService notificationService;

    @EventListener
    public void onProjectStatusChanged(ProjectStatusChangedEvent event) {
        if (event.getUserName() == null) {
            // No destination user — nothing to publish.
            return;
        }
        try {
            var project = event.getProject();
            var status = projectStatusMapper.map(project, event.getProjectModel());
            var projectId = projectIdentifierMapper.map(project);
            notificationService.notifyProjectStatus(event.getUserName(), projectId, project.getBranch(), status);
        } catch (RuntimeException e) {
            log.warn("Failed to publish project status update", e);
        }
    }
}
