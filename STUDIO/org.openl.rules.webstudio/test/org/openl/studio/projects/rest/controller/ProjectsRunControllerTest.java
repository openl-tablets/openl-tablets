package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.projects.messaging.SocketRunExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.run.ExecutionRunResultRegistry;
import org.openl.studio.projects.service.run.RunExecutorService;
import org.openl.studio.projects.service.trace.TableInputParserService;
import org.openl.types.IOpenMethod;

/**
 * Starting a run, refused for a table that does not compile, and reading its result: an accepted request while the
 * run goes on, whatever the client asked for.
 */
class ProjectsRunControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String TABLE_ID = "abc123";
    private static final String TABLE_URI = "file:/Test/rules/Main.xlsx?sheet=Rules&range=B3:E11";
    private static final String MODULE = "Main";
    private static final String INPUT = "{\"hour\":10}";

    private final RulesProject project = mock(RulesProject.class);
    private final ProjectIdModel projectId = ProjectIdModel.builder().repository("design").projectName("Test").build();
    private final ExecutionRunResultRegistry registry = new ExecutionRunResultRegistry();
    private final WorkspaceProjectService projectService = mock(WorkspaceProjectService.class);
    private final RunExecutorService runExecutorService = mock(RunExecutorService.class);
    private final SocketRunExecutionProgressListenerFactory listenerFactory =
            mock(SocketRunExecutionProgressListenerFactory.class);
    private final TableInputParserService inputParserService = mock(TableInputParserService.class);
    private final ProjectModel projectModel = mock(ProjectModel.class);
    private final IOpenMethod method = mock(IOpenMethod.class);
    private ProjectsRunController controller;

    @BeforeEach
    void init() {
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        when(projectService.getUserWorkspace()).thenReturn(mock(UserWorkspace.class));
        controller = new ProjectsRunController(projectService,
                runExecutorService,
                registry,
                listenerFactory,
                inputParserService,
                mock(ProjectObjectMapperService.class),
                projectIdentifierMapper);
    }

    /**
     * A run still going on has nothing to report: the request is accepted rather than refused, so the screen
     * asking after the result raises no error while it waits.
     */
    @Test
    void getResult_whileTheRunIsStillGoingOn() throws Exception {
        registry.setTask(projectId, TABLE_ID, new CompletableFuture<>());

        var answer = controller.getResult(project, true, false, false, JSON);

        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertEquals(ResultNotReadyView.ResultState.NOT_READY, ((ResultNotReadyView) answer.getBody()).status());
    }

    @Test
    void getResult_workbookWhileTheRunIsStillGoingOn() throws Exception {
        registry.setTask(projectId, TABLE_ID, new CompletableFuture<>());

        var answer = controller.getResult(project, true, false, false, XLSX);

        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertNull(answer.getBody(), "a client that asked for a workbook is told by the status alone");
    }

    @Test
    void getResult_withoutARun() {
        assertThrows(NotFoundException.class, () -> controller.getResult(project, true, false, false, JSON));
    }

    @Test
    void getResult_inAMediaTypeItDoesNotProduce() throws Exception {
        registry.setTask(projectId, TABLE_ID, CompletableFuture.completedFuture(mock(TestUnitsResults.class)));

        var answer = controller.getResult(project, true, false, false, MediaType.TEXT_PLAIN_VALUE);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, answer.getStatusCode());
    }

    @Test
    void startRun_refusesATableThatDoesNotCompileInTheProject() {
        givenTable(null, method);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.\nExpected value of type 'Integer'.")));

        var refused = assertThrows(ConflictException.class, () -> controller.startRun(project, TABLE_ID, null, INPUT));

        assertEquals("openl.error.409.run.table.compile.errors.message", refused.getErrorCode());
        assertEquals("Cannot parse cell value 'abc'.", refused.getArgs()[0]);
        assertNothingStarted();
    }

    @Test
    void startRun_refusesATableThatDoesNotCompileInItsModule() {
        givenTable(MODULE, method);
        when(projectModel.getOpenedModuleMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.")));

        assertThrows(ConflictException.class, () -> controller.startRun(project, TABLE_ID, MODULE, INPUT));

        verify(projectModel, never()).getMessagesByTsn(any(), any());
        assertNothingStarted();
    }

    @Test
    void startRun_refusesATableWhoseHeaderDoesNotCompile() {
        givenTable(null, null);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR)).thenReturn(List.of(error("Need to close '('")));

        assertThrows(ConflictException.class, () -> controller.startRun(project, TABLE_ID, null, INPUT));

        assertNothingStarted();
    }

    @Test
    void startRun_answersNotFoundForATableWithoutAMethodAndWithoutErrors() {
        givenTable(null, null);

        assertThrows(NotFoundException.class, () -> controller.startRun(project, TABLE_ID, null, INPUT));

        assertNothingStarted();
    }

    @Test
    void startRun_runsATableThatCompiles() {
        givenTable(null, method);
        givenARunCanStart();

        controller.startRun(project, TABLE_ID, null, INPUT);

        verify(runExecutorService).runMethod(any(), eq(projectModel), any(), any(), any(), eq(false));
        assertTrue(registry.hasTask(projectId));
    }

    @Test
    void startRun_runsWithinTheModuleATableThatHasErrorsOnlyInTheWholeProject() {
        givenTable(MODULE, method);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(error("There can be only one active table")));
        givenARunCanStart();

        controller.startRun(project, TABLE_ID, MODULE, INPUT);

        verify(runExecutorService).runMethod(any(), eq(projectModel), any(), any(), any(), eq(true));
        assertTrue(registry.hasTask(projectId));
    }

    private void givenTable(String fromModule, IOpenMethod tableMethod) {
        var handle = mock(ProjectHandle.class);
        when(projectService.openProject(project, fromModule)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(projectModel);
        var table = mock(IOpenLTable.class);
        when(table.getUri()).thenReturn(TABLE_URI);
        when(projectModel.getTableById(TABLE_ID)).thenReturn(table);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(fromModule == null ? tableMethod : null);
        when(projectModel.getOpenedModuleMethod(TABLE_URI)).thenReturn(fromModule == null ? null : tableMethod);
    }

    private void givenARunCanStart() {
        when(inputParserService.parseInput(eq(INPUT), eq(method), any()))
                .thenReturn(new TableInputParserService.ParseResult(new Object[]{10}, null));
        when(listenerFactory.create(any(), eq(projectId), eq(TABLE_ID)))
                .thenReturn(mock(ExecutionProgressListener.class));
        when(runExecutorService.runMethod(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new CompletableFuture<>());
    }

    private void assertNothingStarted() {
        verifyNoInteractions(inputParserService, listenerFactory, runExecutorService);
        assertFalse(registry.hasTask(projectId));
    }

    private static OpenLMessage error(String summary) {
        return new OpenLMessage(summary, Severity.ERROR);
    }
}
