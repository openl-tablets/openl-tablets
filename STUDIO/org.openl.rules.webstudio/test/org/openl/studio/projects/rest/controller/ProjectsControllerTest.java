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
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.PropertyDefinitionView;
import org.openl.studio.projects.model.PropertyValueView;
import org.openl.studio.projects.model.tables.CopyTableRequest;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.TablePropertiesView;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectMetadataService;
import org.openl.studio.projects.service.ProjectMigrationService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;

class ProjectsControllerTest {

    @Test
    void getProjectPassesIncludesToService() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var expected = mock(ProjectViewModel.class);
        var includes = List.of(ProjectInclude.STATUS, ProjectInclude.DESCRIPTOR);
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

    @Test
    void getModulesDelegatesToProjectService() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var expected = List.of(ModuleViewModel.module("Main", "rules/Main.xlsx"));
        when(projectService.getModules(project)).thenReturn(expected);

        assertEquals(expected, controller.getModules(project));
        verify(projectService).getModules(project);
    }

    @Test
    void getModuleSheetsDelegatesToProjectService() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        when(projectService.getModuleSheets(project, "Main")).thenReturn(List.of("Rules", "Data"));

        assertEquals(List.of("Rules", "Data"), controller.getModuleSheets(project, "Main"));
        verify(projectService).getModuleSheets(project, "Main");
    }

    @Test
    void getPropertiesDelegatesToMetadataService() {
        var metadataService = mock(ProjectMetadataService.class);
        var controller = controller(mock(WorkspaceProjectService.class), mock(ProjectStatusMapper.class),
                metadataService);
        var expected = List.of(new PropertyDefinitionView(
                "state", "enum", true, List.of(new PropertyValueView("AL", "Alabama"))));
        when(metadataService.getProperties("Rules")).thenReturn(expected);

        assertEquals(expected, controller.getProperties(mock(RulesProject.class), "Rules"));
        verify(metadataService).getProperties("Rules");
    }

    @Test
    void createNewTableReadsTheResponseByTheWrittenTableId() throws ProjectException {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var table = RawTableView.builder()
                .kind(TableKind.CONSTANTS)
                .name("Constants")
                .source(List.of())
                .build();
        var request = new CreateNewTableRequest("Main", "Rules", null, table);
        var expected = SummaryTableView.builder()
                .id("created-id")
                .tableType("RawSource")
                .kind(TableKind.CONSTANTS)
                .name("Constants")
                .build();
        when(projectService.createNewTable(project, request)).thenReturn("created-id");
        when(projectService.getCreatedTable(project, "Main", "created-id", "Constants")).thenReturn(expected);

        var created = controller.createNewTable(project, request);

        assertEquals(expected, created);
        verify(projectService).getCreatedTable(project, "Main", "created-id", "Constants");
    }

    @Test
    void copyTableReadsTheResponseByTheCopyName() throws ProjectException {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var request = new CopyTableRequest("Main", "Rules", null, "GreetingCopy", null);
        var expected = SummaryTableView.builder()
                .id("copy-id")
                .tableType("Rules")
                .kind(TableKind.RULES)
                .name("GreetingCopy")
                .build();
        when(projectService.copyTable(project, "source-id", request)).thenReturn("copy-id");
        when(projectService.getCreatedTable(project, "Main", "copy-id", "GreetingCopy")).thenReturn(expected);

        var copied = controller.copyTable(project, "source-id", request);

        assertEquals(expected, copied);
        verify(projectService).copyTable(project, "source-id", request);
        // The id the copy returns is read back, not the source name a same-named copy would collide on.
        verify(projectService).getCreatedTable(project, "Main", "copy-id", "GreetingCopy");
    }

    @Test
    void getTablePropertiesDelegatesToProjectService() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var expected = new TablePropertiesView("Greeting", TableKind.RULES, List.of());
        when(projectService.getTableProperties(project, "table-id")).thenReturn(expected);

        assertEquals(expected, controller.getTableProperties(project, "table-id"));
        verify(projectService).getTableProperties(project, "table-id");
    }

    private static ProjectsController controller(WorkspaceProjectService projectService,
                                                 ProjectStatusMapper projectStatusMapper) {
        return controller(projectService, projectStatusMapper, mock(ProjectMetadataService.class));
    }

    private static ProjectsController controller(WorkspaceProjectService projectService,
                                                 ProjectStatusMapper projectStatusMapper,
                                                 ProjectMetadataService metadataService) {
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
                mock(ProjectTablesGraphService.class),
                mock(RepositoryConfigService.class),
                metadataService,
                mock(ProjectMigrationService.class),
                mock(ProjectRevisionService.class)) {
            @Override
            public WebStudio getWebStudio() {
                return webStudio;
            }
        };
    }
}
