package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableUtils;
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
import org.openl.studio.projects.model.tests.TestUnitExecutionResult;
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
import org.openl.studio.projects.service.tests.RetainedTestUnit;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.projects.service.tests.TestsRerun;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Reading the outcome of a test run: every test table as it is announced, the summary once the run has ended, and
 * an accepted request while it goes on.
 */
class ProjectsControllerTestsSummaryTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String TABLE_URI = "file://test.xlsx#Sheet1!A1";
    private static final String TABLE_ID = TableUtils.makeTableId(TABLE_URI);

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

    /** Runs no case again: the project was compiled again since the run. */
    private static final TestsRerun COMPILED_AGAIN = (method, test) -> null;

    /** A test run that has ended with the given tables. */
    private void ended(List<TestUnitsResults> tables) {
        registry.setTask(projectId, CompletableFuture.completedFuture(tables));
    }

    private static void assertNotReady(ResponseEntity<?> answer) {
        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertEquals(ResultNotReadyView.ResultState.NOT_READY, ((ResultNotReadyView) answer.getBody()).status());
    }

    @Test
    void getTestsSummary_onceTheTestsHaveEnded() throws Exception {
        ended(List.of());

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

    /**
     * The workbook holds what every case of a Run table returned. A case that gave its value back to free memory
     * runs again while its row is written; a case that holds its value is written as it is.
     */
    @Test
    void getTestsSummary_workbookOfARunTableWhoseValueWasReleased() throws Exception {
        var suite = testSuiteNamed("DriverRun");
        var premium = Map.of("premium", 100);
        var kept = retained(returning("1", premium));
        var ranAgain = new ArrayList<TestDescription>();
        var released = released(suite, returning("2", Map.of("premium", 150)), (method, test) -> {
            ranAgain.add(test);
            return returning("2", Map.of("premium", 150));
        });
        ended(List.of(runTableOf(suite, kept, released)));

        var answer = summary(XLSX);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream((byte[]) answer.getBody()))) {
            var formatter = new DataFormatter();
            var written = StreamSupport.stream(workbook.getSheet("Result 1").spliterator(), false)
                    .flatMap(row -> StreamSupport.stream(row.spliterator(), false))
                    .map(formatter::formatCellValue)
                    .toList();
            assertTrue(written.stream().anyMatch(cell -> cell.contains("100")), written::toString);
            assertTrue(written.stream().anyMatch(cell -> cell.contains("150")), written::toString);
        }
        assertEquals(List.of(released.getTest()), ranAgain);
        assertTrue(RetainedTestUnit.isReleased(released.getActualResult()), "the run keeps what it kept");
        Reference.reachabilityFence(premium);
    }

    /**
     * A value that was given back to free memory cannot be had once the project was compiled again, and the
     * workbook is not written without it.
     */
    @Test
    void getTestsSummary_workbookOfATableWhoseValueWasReleasedOnceTheProjectWasCompiledAgain() {
        var suite = testSuiteNamed("DriverRun");
        ended(List.of(runTableOf(suite, released(suite, returning(Map.of("premium", 150)), COMPILED_AGAIN))));

        var refusal = assertThrows(NotFoundException.class, () -> summary(XLSX));
        assertEquals("openl.error.404.tests.execution.values.released.message", refusal.getErrorCode());
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
        ended(List.of());

        assertThrows(NotFoundException.class, () -> controller.getTestCaseResult(project, "table", "case"));
    }

    /** A case that still holds its values is read as it is kept, and nothing is run again. */
    @Test
    void getTestCaseResult_ofACaseThatHoldsItsValues() {
        var premium = Map.of("premium", 150);
        ended(List.of(runTableOf(testSuiteNamed("DriverRun"), retained(returning(premium)))));

        var answer = (TestUnitExecutionResult) controller.getTestCaseResult(project, TABLE_ID, "1").getBody();

        assertEquals(150, answer.result().value().get("premium").asInt());
        Reference.reachabilityFence(premium);
    }

    /**
     * A case whose value was given back to free memory is run again, and what that returns is read. The run keeps
     * what it kept.
     */
    @Test
    void getTestCaseResult_ofACaseWhoseValueWasReleased() {
        var suite = testSuiteNamed("DriverRun");
        var ranAgain = returning(Map.of("premium", 150));
        var unit = returning(Map.of("premium", 150));
        var kept = released(suite, unit,
                (method, test) -> method == suite.getTestSuiteMethod() && test == unit.getTest() ? ranAgain : null);
        ended(List.of(runTableOf(suite, kept)));

        var answer = (TestUnitExecutionResult) controller.getTestCaseResult(project, TABLE_ID, "1").getBody();

        assertEquals(150, answer.result().value().get("premium").asInt());
        assertTrue(RetainedTestUnit.isReleased(kept.getActualResult()));
    }

    /** A value given back to free memory cannot be had once the project was compiled again. */
    @Test
    void getTestCaseResult_ofACaseWhoseValueWasReleasedOnceTheProjectWasCompiledAgain() {
        var suite = testSuiteNamed("DriverRun");
        ended(List.of(runTableOf(suite, released(suite, returning(Map.of("premium", 150)), COMPILED_AGAIN))));

        var refusal = assertThrows(NotFoundException.class,
                () -> controller.getTestCaseResult(project, TABLE_ID, "1"));
        assertEquals("openl.error.404.tests.execution.values.released.message", refusal.getErrorCode());
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
        var testSuite = testSuiteNamed("DriverTest");
        var testUnit = mock(ITestUnit.class);
        var test = mock(TestDescription.class);
        when(results.getTestSuite()).thenReturn(testSuite);
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

    /** A test table of the given name, at the cell {@link #TABLE_ID} is made from. */
    private static TestSuite testSuiteNamed(String name) {
        var testSuite = mock(TestSuite.class);
        var testMethod = mock(TestSuiteMethod.class);
        var testedMethod = mock(IOpenMethod.class);
        var signature = mock(IMethodSignature.class);
        var methodInfo = mock(IMemberMetaInfo.class);
        var syntaxNode = mock(TableSyntaxNode.class);
        var properties = mock(ITableProperties.class);
        when(testSuite.getTestSuiteMethod()).thenReturn(testMethod);
        when(testSuite.getUri()).thenReturn(TABLE_URI);
        when(testSuite.getTestedMethod()).thenReturn(testedMethod);
        when(testedMethod.getSignature()).thenReturn(signature);
        when(signature.getParameterTypes()).thenReturn(new IOpenClass[0]);
        when(testMethod.getInfo()).thenReturn(methodInfo);
        when(testMethod.getSyntaxNode()).thenReturn(syntaxNode);
        when(methodInfo.getSyntaxNode()).thenReturn(syntaxNode);
        when(syntaxNode.getTableProperties()).thenReturn(properties);
        when(properties.getName()).thenReturn(name);
        return testSuite;
    }

    /** The results of a Run table of the given suite, with the given cases. */
    private static TestUnitsResults runTableOf(TestSuite suite, ITestUnit... units) {
        var results = mock(TestUnitsResults.class);
        var tests = Stream.of(units).map(ITestUnit::getTest).toArray(TestDescription[]::new);
        when(suite.getTestSuiteMethod().getTests()).thenReturn(tests);
        when(suite.getTestSuiteMethod().isRunMethod()).thenReturn(true);
        when(results.getTestSuite()).thenReturn(suite);
        when(results.isRunmethod()).thenReturn(true);
        when(results.getTestUnits()).thenReturn(List.of(units));
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getTestResultColumnDisplayNames()).thenReturn(new String[0]);
        return results;
    }

    /** A case of a Run table, the first one, that returned the given value. */
    private static ITestUnit returning(Object value) {
        return returning("1", value);
    }

    /** A case of a Run table that returned the given value. */
    private static ITestUnit returning(String id, Object value) {
        var unit = mock(ITestUnit.class);
        var test = mock(TestDescription.class);
        when(unit.getTest()).thenReturn(test);
        when(unit.getResultStatus()).thenReturn(TestStatus.TR_OK);
        when(unit.getActualResult()).thenReturn(value);
        when(unit.getActualParam()).thenReturn(new ParameterWithValueDeclaration("actual", value));
        when(unit.getComparisonResults()).thenReturn(List.of());
        when(unit.getContextParams(any())).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(test.getId()).thenReturn(id);
        when(test.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        return unit;
    }

    /** The case as a run keeps it, its value still held: it runs nothing again. */
    private static RetainedTestUnit retained(ITestUnit unit) {
        return new RetainedTestUnit(unit, mock(TestSuiteMethod.class), (method, test) -> {
            throw new AssertionError("nothing is run again");
        }, SoftReference::new);
    }

    /** A case of the test table as a run keeps it, once its value was given back: it runs again the given way. */
    private static RetainedTestUnit released(TestSuite suite, ITestUnit unit, TestsRerun rerun) {
        return new RetainedTestUnit(unit, suite.getTestSuiteMethod(), rerun, value -> new SoftReference<>(null));
    }
}
