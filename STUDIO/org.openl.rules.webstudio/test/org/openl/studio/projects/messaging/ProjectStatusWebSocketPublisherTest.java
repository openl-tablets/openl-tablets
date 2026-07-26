package org.openl.studio.projects.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectStatusChangedEvent;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;

@ExtendWith(MockitoExtension.class)
class ProjectStatusWebSocketPublisherTest {

    @Mock
    private ProjectStatusMapper projectStatusMapper;

    @Mock
    private ProjectIdentifierMapper projectIdentifierMapper;

    @Mock
    private ProjectSocketNotificationService notificationService;

    @InjectMocks
    private ProjectStatusWebSocketPublisher publisher;

    @Test
    void a_status_change_reaches_the_project_destination_and_the_workspace_stream() {
        var project = mock(RulesProject.class);
        when(project.getBranch()).thenReturn("main");
        var event = mock(ProjectStatusChangedEvent.class);
        when(event.getUserName()).thenReturn("jane");
        when(event.getProject()).thenReturn(project);
        var status = mock(ProjectStatusViewModel.class);
        when(projectStatusMapper.map(project, event.getProjectModel())).thenReturn(status);
        var projectId = ProjectIdModel.builder().repository("design").projectName("Alpha").build();
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);

        publisher.onProjectStatusChanged(event);

        // The single-project screens keep their precise destination…
        verify(notificationService).notifyProjectStatus("jane", projectId, "main", status);
        // …and the projects list reads the same status from the one workspace-wide stream.
        verify(notificationService).notifyWorkspaceProjectStatus("jane", status);
    }

    @Test
    void a_status_change_without_a_destination_user_goes_nowhere() {
        var event = mock(ProjectStatusChangedEvent.class);
        when(event.getUserName()).thenReturn(null);

        publisher.onProjectStatusChanged(event);

        verifyNoInteractions(notificationService);
    }
}
