package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Page;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
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
import org.openl.studio.projects.model.tables.TableInputView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.TablePropertiesView;
import org.openl.studio.projects.model.tables.TestCaseView;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectMetadataService;
import org.openl.studio.projects.service.ProjectMigrationService;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.TableInputService;
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
        var expected = List.of(new PropertyDefinitionView("state", "US States", "Business Dimension", "enum", true,
                true, null, null, List.of(new PropertyValueView("AL", "Alabama"))));
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
        var expected = new TablePropertiesView("Greeting", TableKind.RULES, List.of(), null);
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
        return controller(projectService, projectStatusMapper, metadataService, mock(TableInputService.class),
                mock(ProjectObjectMapperService.class));
    }

    private static ProjectsController controller(WorkspaceProjectService projectService,
                                                 ProjectStatusMapper projectStatusMapper,
                                                 ProjectMetadataService metadataService,
                                                 TableInputService tableInputService,
                                                 ProjectObjectMapperService objectMapperService) {
        var webStudio = mock(WebStudio.class);
        return new ProjectsController(
                projectService,
                mock(TestsExecutorService.class),
                mock(ExecutionTestsResultRegistry.class),
                mock(SocketProjectAllTestsExecutionProgressListenerFactory.class),
                objectMapperService,
                mock(ProjectsMergeConflictsSessionHolder.class),
                mock(ProjectIdentifierMapper.class),
                projectStatusMapper,
                mock(ProjectTablesGraphService.class),
                mock(RepositoryConfigService.class),
                metadataService,
                mock(ProjectMigrationService.class),
                mock(ProjectRevisionService.class),
                tableInputService) {
            @Override
            public WebStudio getWebStudio() {
                return webStudio;
            }
        };
    }

    @Test
    void getTableInputDescribesTheCompiledTableThroughTheService() {
        var projectService = mock(WorkspaceProjectService.class);
        var tableInputService = mock(TableInputService.class);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class),
                mock(ProjectMetadataService.class), tableInputService, objectMapperService);
        var project = mock(RulesProject.class);
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        var table = mock(IOpenLTable.class);
        var objectMapper = new ObjectMapper();
        var expected = TableInputView.builder().name("Premium").build();
        when(projectService.openProject(project, "Main")).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getTableById("t1")).thenReturn(table);
        when(objectMapperService.createObjectMapper()).thenReturn(objectMapper);
        when(tableInputService.describe(model, table, true, objectMapper, null)).thenReturn(expected);

        assertEquals(expected, controller.getTableInput(project, "t1", "Main"));
    }

    @Test
    void getTableInputReadsTheWholeProjectWhenTheModuleIsBlank() {
        var projectService = mock(WorkspaceProjectService.class);
        var tableInputService = mock(TableInputService.class);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class),
                mock(ProjectMetadataService.class), tableInputService, objectMapperService);
        var project = mock(RulesProject.class);
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        var table = mock(IOpenLTable.class);
        var objectMapper = new ObjectMapper();
        var expected = TableInputView.builder().name("Premium").build();
        // A blank module means the whole project, not a module named "", which would fail to resolve.
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getTableById("t1")).thenReturn(table);
        when(objectMapperService.createObjectMapper()).thenReturn(objectMapper);
        when(tableInputService.describe(model, table, false, objectMapper, null)).thenReturn(expected);

        assertEquals(expected, controller.getTableInput(project, "t1", "  "));
    }

    @Test
    void getTableInputCasesListsThePageThroughTheService() {
        var projectService = mock(WorkspaceProjectService.class);
        var tableInputService = mock(TableInputService.class);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class),
                mock(ProjectMetadataService.class), tableInputService, objectMapperService);
        var project = mock(RulesProject.class);
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        var table = mock(IOpenLTable.class);
        var objectMapper = new ObjectMapper();
        var page = Page.of(1, 25);
        var expected = PageResponse.of(List.of(TestCaseView.builder().id("7").build()), page, 30L);
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getTableById("t1")).thenReturn(table);
        when(objectMapperService.createObjectMapper()).thenReturn(objectMapper);
        when(tableInputService.listTestCases(model, table, false, page, objectMapper, null)).thenReturn(expected);

        assertEquals(expected, controller.getTableInputCases(project, "t1", null, page));
    }

    @Test
    void getTableInputCaseDescribesTheCaseThroughTheService() {
        var projectService = mock(WorkspaceProjectService.class);
        var tableInputService = mock(TableInputService.class);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class),
                mock(ProjectMetadataService.class), tableInputService, objectMapperService);
        var project = mock(RulesProject.class);
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        var table = mock(IOpenLTable.class);
        var objectMapper = new ObjectMapper();
        var expected = TestCaseView.builder().id("7").build();
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getTableById("t1")).thenReturn(table);
        when(objectMapperService.createObjectMapper()).thenReturn(objectMapper);
        when(tableInputService.describeTestCase(model, table, false, "7", objectMapper, null)).thenReturn(expected);

        assertEquals(expected, controller.getTableInputCase(project, "t1", "7", null));
    }

    @Test
    void getTableInputCaseRejectsUnknownTable() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getTableById("missing")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> controller.getTableInputCase(project, "missing", "1", null));
    }

    @Test
    void getTableInputRejectsUnknownTable() {
        var projectService = mock(WorkspaceProjectService.class);
        var controller = controller(projectService, mock(ProjectStatusMapper.class));
        var project = mock(RulesProject.class);
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getTableById("missing")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> controller.getTableInput(project, "missing", null));
    }
}
