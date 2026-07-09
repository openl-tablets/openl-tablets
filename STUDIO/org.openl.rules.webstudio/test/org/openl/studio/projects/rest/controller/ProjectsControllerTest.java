package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.messaging.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.TestsExecutorService;

class ProjectsControllerTest {

    @Test
    void getProjectPassesIncludesToService() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var expected = mock(ProjectViewModel.class);
        var includes = List.of(ProjectInclude.STATUS, ProjectInclude.MODULES);
        when(projectService.getProject(project, includes)).thenReturn(expected);

        var result = controller.getProject(project, includes);

        assertEquals(expected, result);
        verify(projectService).getProject(project, includes);
    }

    @Test
    void updateProjectStatusKeepsSaveRequestWithoutGeneratedComment() throws ProjectException {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var request = ProjectStatusUpdateModel.builder()
                .save(true)
                .comment(" ")
                .build();

        controller.updateProjectStatus(project, request);

        var captor = ArgumentCaptor.forClass(ProjectStatusUpdateModel.class);
        verify(projectService).updateProjectStatus(eq(project), captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().save());
        assertNull(captor.getValue().comment());
    }

    @Test
    void deleteProjectDelegatesToHardDeleteService() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);

        controller.deleteProject(project, "comment");

        verify(projectService).delete(project, "comment");
    }

    private static ProjectsController controller(WorkspaceProjectService projectService,
                                                 ProjectStatusMapper projectStatusMapper) {
        var webStudio = mock(WebStudio.class);
        return new ProjectsController(
                projectService,
                mock(TestsExecutorService.class),
                mock(ExecutionTestsResultRegistry.class),
                mock(SocketProjectAllTestsExecutionProgressListenerFactory.class),
                mock(Environment.class),
                mock(ProjectsMergeConflictsSessionHolder.class),
                mock(ProjectIdentifierMapper.class),
                projectStatusMapper,
                mock(ProjectTablesGraphService.class)) {
            @Override
            public WebStudio getWebStudio() {
                return webStudio;
            }
        };
    }
}
