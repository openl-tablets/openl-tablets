package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketBenchmarkExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.benchmark.BenchmarkExecutorService;
import org.openl.studio.projects.service.benchmark.BenchmarkMeasurement;
import org.openl.studio.projects.service.benchmark.ExecutionBenchmarkResultRegistry;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.trace.TableInputParserService;
import org.openl.types.IOpenMethod;

class ProjectsBenchmarkControllerTest {

    private static final String TABLE_ID = "abc123";
    private static final String TABLE_URI = "file://test.xlsx#Sheet1!A1";

    private RulesProject project;
    private ProjectModel model;
    private IOpenLTable table;
    private BenchmarkExecutorService benchmarkExecutorService;
    private ExecutionBenchmarkResultRegistry registry;
    private TableInputParserService inputParserService;
    private ExecutionProgressListener listener;
    private ProjectIdModel projectId;
    private ProjectsBenchmarkController controller;

    private static BenchmarkMeasurement measurement() {
        return new BenchmarkMeasurement("m1", TABLE_ID, "PolicyTest", true, null, 2, 128, 3_200_000_000L, List.of());
    }

    private void tableIsExecutedBy(IOpenMethod method) {
        lenient().when(model.getMethod(TABLE_URI)).thenReturn(method);
        lenient().when(model.getOpenedModuleMethod(TABLE_URI)).thenReturn(method);
    }

    @BeforeEach
    void init() {
        project = mock(RulesProject.class);
        table = mock(IOpenLTable.class);
        lenient().when(table.getUri()).thenReturn(TABLE_URI);

        model = mock(ProjectModel.class);
        lenient().when(model.getTableById(TABLE_ID)).thenReturn(table);
        var handle = mock(ProjectHandle.class);
        lenient().when(handle.awaitCompiled()).thenReturn(model);

        var projectService = mock(WorkspaceProjectService.class);
        var userWorkspace = mock(UserWorkspace.class);
        lenient().when(projectService.openProject(eq(project), any())).thenReturn(handle);
        lenient().when(projectService.getUserWorkspace()).thenReturn(userWorkspace);

        projectId = ProjectIdModel.builder().repository("design").projectName("TestProject").build();
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        lenient().when(projectIdentifierMapper.map(project)).thenReturn(projectId);

        listener = mock(ExecutionProgressListener.class);
        var listenerFactory = mock(SocketBenchmarkExecutionProgressListenerFactory.class);
        lenient().when(listenerFactory.create(any(), any(), any())).thenReturn(listener);

        benchmarkExecutorService = mock(BenchmarkExecutorService.class);
        lenient().when(benchmarkExecutorService.benchmark(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(List.of(measurement())));

        registry = new ExecutionBenchmarkResultRegistry();
        inputParserService = mock(TableInputParserService.class);
        var objectMapperService = mock(ProjectObjectMapperService.class);
        lenient().when(objectMapperService.createObjectMapper()).thenReturn(new ObjectMapper());

        controller = new ProjectsBenchmarkController(projectService,
                benchmarkExecutorService,
                registry,
                listenerFactory,
                inputParserService,
                objectMapperService,
                projectIdentifierMapper);
    }

    @Test
    void startBenchmark_testTableIsMeasuredOverTheCasesItStates() {
        tableIsExecutedBy(mock(TestSuiteMethod.class));

        controller.startBenchmark(project, TABLE_ID, "2-4", null, null);

        verify(listener).onStatusChanged(ExecutionStatus.PENDING);
        // A test table carries its own input, so nothing is parsed from the request.
        verify(inputParserService, never()).parseInput(any(), any(), any());
        verify(benchmarkExecutorService).benchmark(eq(listener), eq(model), eq(table), eq("2-4"), isNull(), isNull(),
                eq(false));
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void startBenchmark_anyOtherTableIsMeasuredOverTheInputItIsGiven() {
        tableIsExecutedBy(mock(IOpenMethod.class));
        var params = new Object[]{42};
        when(inputParserService.parseInput(eq("{}"), any(), any()))
                .thenReturn(new TableInputParserService.ParseResult(params, null));

        controller.startBenchmark(project, TABLE_ID, null, "Main", "{}");

        verify(benchmarkExecutorService).benchmark(eq(listener), eq(model), eq(table), isNull(), eq(params), isNull(),
                eq(true));
    }

    @Test
    void startBenchmark_blankModuleMeansTheWholeProject() {
        tableIsExecutedBy(mock(TestSuiteMethod.class));

        // A blank module means the whole project, not a module named "", which would fail to resolve.
        controller.startBenchmark(project, TABLE_ID, null, "  ", null);

        verify(benchmarkExecutorService).benchmark(eq(listener), eq(model), eq(table), isNull(), isNull(), isNull(),
                eq(false));
    }

    @Test
    void startBenchmark_tableNotFound() {
        when(model.getTableById(TABLE_ID)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> controller.startBenchmark(project, TABLE_ID, null, null, null));
    }

    @Test
    void startBenchmark_tableIsNotExecutable() {
        tableIsExecutedBy(null);

        assertThrows(NotFoundException.class, () -> controller.startBenchmark(project, TABLE_ID, null, null, null));
    }

    @Test
    void getBenchmarks_reportsWhatTheSessionHasMeasured() {
        tableIsExecutedBy(mock(TestSuiteMethod.class));
        controller.startBenchmark(project, TABLE_ID, null, null, null);

        var results = controller.getBenchmarks(project);

        assertEquals(1, results.size());
        var result = results.getFirst();
        assertEquals("m1", result.id());
        assertEquals("PolicyTest", result.name());
        assertEquals(2, result.testCases());
        assertEquals(128, result.runs());
        assertEquals(3200.0, result.executionTimeMs(), 0.001);
    }

    @Test
    void getBenchmarks_whileTheMeasurementIsStillGoingOn() {
        tableIsExecutedBy(mock(TestSuiteMethod.class));
        when(benchmarkExecutorService.benchmark(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new CompletableFuture<>());
        controller.startBenchmark(project, TABLE_ID, null, null, null);

        assertThrows(ConflictException.class, () -> controller.getBenchmarks(project));
    }

    @Test
    void getBenchmarks_nothingMeasuredYet() {
        assertTrue(controller.getBenchmarks(project).isEmpty());
    }

    @Test
    void deleteBenchmarks_forgetsTheNamedMeasurements() {
        tableIsExecutedBy(mock(TestSuiteMethod.class));
        controller.startBenchmark(project, TABLE_ID, null, null, null);
        controller.getBenchmarks(project);

        controller.deleteBenchmarks(project, List.of("m1"));

        assertTrue(controller.getBenchmarks(project).isEmpty());
    }

    @Test
    void deleteBenchmarks_forgetsEverythingAndCancelsTheMeasurement() {
        tableIsExecutedBy(mock(TestSuiteMethod.class));
        var running = new CompletableFuture<List<BenchmarkMeasurement>>();
        when(benchmarkExecutorService.benchmark(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(running);
        controller.startBenchmark(project, TABLE_ID, null, null, null);

        controller.deleteBenchmarks(project, null);

        assertTrue(running.isCancelled());
        assertTrue(controller.getBenchmarks(project).isEmpty());
        assertNull(registry.getResultIfDone(projectId));
    }
}
