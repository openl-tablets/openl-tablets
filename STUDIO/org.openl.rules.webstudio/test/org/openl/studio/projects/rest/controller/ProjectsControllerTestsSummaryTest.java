package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.messaging.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.tests.TestsExecutionSummary;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectMetadataService;
import org.openl.studio.projects.service.ProjectMigrationService;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.TableInputService;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;

/**
 * Reading the outcome of a test run: the summary once it has ended, and an accepted request while it goes on.
 */
class ProjectsControllerTestsSummaryTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final RulesProject project = mock(RulesProject.class);
    private final ProjectIdModel projectId = ProjectIdModel.builder().repository("design").projectName("Test").build();
    private final ExecutionTestsResultRegistry registry = new ExecutionTestsResultRegistry();
    private ProjectsController controller;

    @BeforeEach
    void init() {
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        when(objectMapperService.createObjectMapper()).thenReturn(new ObjectMapper());
        controller = new ProjectsController(mock(WorkspaceProjectService.class),
                mock(TestsExecutorService.class),
                registry,
                mock(SocketProjectAllTestsExecutionProgressListenerFactory.class),
                objectMapperService,
                mock(ProjectsMergeConflictsSessionHolder.class),
                projectIdentifierMapper,
                mock(ProjectStatusMapper.class),
                mock(ProjectTablesGraphService.class),
                mock(RepositoryConfigService.class),
                mock(ProjectMetadataService.class),
                mock(ProjectMigrationService.class),
                mock(ProjectRevisionService.class),
                mock(TableInputService.class)) {
            @Override
            protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
                return new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
            }
        };
    }

    private ResponseEntity<?> summary(String acceptMediaType) throws Exception {
        return controller.getTestsSummary(
                project, false, 5, false, false, false, Pageable.unpaged(), acceptMediaType);
    }

    private static void assertNotReady(ResponseEntity<?> answer) {
        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertEquals(ResultNotReadyView.ResultState.NOT_READY, ((ResultNotReadyView) answer.getBody()).status());
    }

    @Test
    void getTestsSummary_onceTheTestsHaveEnded() throws Exception {
        registry.setTask(projectId, CompletableFuture.completedFuture(List.of()));

        var answer = summary(JSON);

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertEquals(0, ((TestsExecutionSummary) answer.getBody()).getNumberOfTests());
    }

    /**
     * Tests still running have nothing to report: the request is accepted rather than refused, so the screen
     * asking after the summary raises no error while it waits.
     */
    @Test
    void getTestsSummary_whileTheTestsAreStillRunning() throws Exception {
        registry.setTask(projectId, new CompletableFuture<>());

        assertNotReady(summary(JSON));
    }

    @Test
    void getTestsSummary_workbookWhileTheTestsAreStillRunning() throws Exception {
        registry.setTask(projectId, new CompletableFuture<>());

        var answer = summary(XLSX);

        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertNull(answer.getBody(), "a client that asked for a workbook is told by the status alone");
    }

    @Test
    void getTestsSummary_withoutATestRun() {
        assertThrows(NotFoundException.class, () -> summary(JSON));
    }

    @Test
    void getTestCaseResult_whileTheTestsAreStillRunning() {
        registry.setTask(projectId, new CompletableFuture<>());

        assertNotReady(controller.getTestCaseResult(project, "table", "case"));
    }

    @Test
    void getTestCaseResult_theRunHoldsNoSuchTable() {
        registry.setTask(projectId, CompletableFuture.completedFuture(List.of()));

        assertThrows(NotFoundException.class, () -> controller.getTestCaseResult(project, "table", "case"));
    }
}
