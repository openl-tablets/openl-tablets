package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.messaging.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.tests.TestCaseExecutionResult;
import org.openl.studio.projects.model.tests.TestsExecutionSummary;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectMetadataService;
import org.openl.studio.projects.service.ProjectMigrationService;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.TableInputService;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.ProjectTestsExecutionProgressListener;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;

/**
 * Reading the outcome of a test run: every test table as it is announced, the summary once the run has ended, and
 * an accepted request while it goes on.
 */
class ProjectsControllerTestsSummaryTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final RulesProject project = mock(RulesProject.class);
    private final ProjectIdModel projectId = ProjectIdModel.builder().repository("design").projectName("Test").build();
    private final ExecutionTestsResultRegistry registry = new ExecutionTestsResultRegistry();
    private final WorkspaceProjectService projectService = mock(WorkspaceProjectService.class);
    private final SocketProjectAllTestsExecutionProgressListenerFactory listeners =
            mock(SocketProjectAllTestsExecutionProgressListenerFactory.class);
    private ProjectsController controller;

    @BeforeEach
    void init() {
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        when(objectMapperService.createObjectMapper()).thenReturn(new ObjectMapper());
        controller = new ProjectsController(projectService,
                mock(TestsExecutorService.class),
                registry,
                listeners,
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

    /**
     * A test table that has run is announced the way the screen reads the results: an input with inner structure
     * is referred to instead of written, and no schema is written.
     */
    @Test
    void runAllTests_announcesATestTableWithItsValuesReferredTo() {
        var handle = mock(ProjectHandle.class);
        var model = mock(ProjectModel.class);
        var workspace = mock(UserWorkspace.class);
        var listener = mock(ProjectTestsExecutionProgressListener.class);
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(projectService.getUserWorkspace()).thenReturn(workspace);
        when(projectService.getTableModules(project)).thenReturn(TableModules.none());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<TestUnitsResults, TestCaseExecutionResult>> announcement =
                ArgumentCaptor.forClass(Function.class);
        when(listeners.create(any(), any(), announcement.capture())).thenReturn(listener);

        controller.runAllTests(project, null, null, null);

        var input = announcement.getValue().apply(testTableGiven(Map.of("name", "Sara")))
                .testUnits().getFirst().parameters().getFirst();
        assertEquals(Boolean.TRUE, input.lazy());
        assertNull(input.value(), "the value is read when the screen asks for it");
        assertNull(input.schema(), "the screen reads no schema");
    }

    /** The results of a test table of one case that passed with the given input. */
    private static TestUnitsResults testTableGiven(Object input) {
        var results = mock(TestUnitsResults.class);
        var testSuite = mock(TestSuite.class);
        var testMethod = mock(TestSuiteMethod.class);
        var methodInfo = mock(IMemberMetaInfo.class);
        var syntaxNode = mock(TableSyntaxNode.class);
        var properties = mock(ITableProperties.class);
        var testUnit = mock(ITestUnit.class);
        var test = mock(TestDescription.class);
        when(results.getTestSuite()).thenReturn(testSuite);
        when(testSuite.getTestSuiteMethod()).thenReturn(testMethod);
        when(testSuite.getUri()).thenReturn("file://test.xlsx#Sheet1!A1");
        when(testMethod.getInfo()).thenReturn(methodInfo);
        when(testMethod.getSyntaxNode()).thenReturn(syntaxNode);
        when(methodInfo.getSyntaxNode()).thenReturn(syntaxNode);
        when(syntaxNode.getTableProperties()).thenReturn(properties);
        when(properties.getName()).thenReturn("DriverTest");
        when(results.getFilteredTestUnits(false, 5)).thenReturn(List.of(testUnit));
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[]{"Driver"});
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getTestResultColumnDisplayNames()).thenReturn(new String[0]);
        when(testUnit.getTest()).thenReturn(test);
        when(testUnit.getResultStatus()).thenReturn(TestStatus.TR_OK);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(test.getExecutionParams()).thenReturn(new ParameterWithValueDeclaration[]{
                new ParameterWithValueDeclaration("driver", input, mock(IOpenClass.class))});
        return results;
    }
}
