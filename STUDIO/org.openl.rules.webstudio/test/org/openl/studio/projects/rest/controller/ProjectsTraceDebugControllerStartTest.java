package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketDebugListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.trace.StackViewMode;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.trace.DebugSessionRegistry;
import org.openl.studio.projects.service.trace.TraceDebugService;
import org.openl.studio.projects.service.trace.TraceExportService;
import org.openl.studio.projects.service.trace.TraceHighlightService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethod;

class ProjectsTraceDebugControllerStartTest {

    private static final String ERROR_CODE = "openl.error.409.trace.table.compile.errors.message";
    private static final String TABLE_ID = "abc123";
    private static final String TABLE_URI = "file:/Test/rules/Main.xlsx?sheet=Rules&range=B3:E11";
    private static final String TARGET_URI = "file:/Test/rules/Main.xlsx?sheet=Rules&range=B15:E23";
    private static final String OTHER_TARGET_URI = "file:/Test/rules/Main.xlsx?sheet=Rules&range=B27:E35";
    private static final String MODULE = "Main";
    private static final String INPUT = "{\"hour\":10}";

    private final RulesProject project = mock(RulesProject.class);
    private final ProjectIdModel projectId = ProjectIdModel.builder().repository("design").projectName("Test").build();
    private final WorkspaceProjectService projectService = mock(WorkspaceProjectService.class);
    private final TraceDebugService traceDebugService = mock(TraceDebugService.class);
    private final ProjectModel projectModel = mock(ProjectModel.class);
    private final IOpenLTable table = mock(IOpenLTable.class);
    private final IOpenMethod method = mock(IOpenMethod.class);
    private final IllegalStateException sessionStarted = new IllegalStateException("session started");
    private ProjectsTraceDebugController controller;

    @BeforeEach
    void init() {
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        var workspace = mock(UserWorkspace.class);
        when(projectService.getUserWorkspace()).thenReturn(workspace);
        when(table.getUri()).thenReturn(TABLE_URI);
        when(traceDebugService.startSession(any())).thenThrow(sessionStarted);
        controller = new ProjectsTraceDebugController(
                projectService,
                projectIdentifierMapper,
                traceDebugService,
                mock(DebugSessionRegistry.class),
                mock(SocketDebugListenerFactory.class),
                mock(TraceParameterRegistry.class),
                mock(TraceHighlightService.class),
                mock(TraceExportService.class),
                mock(ProjectTablesGraphService.class),
                mock(ProjectObjectMapperService.class));
    }

    @Test
    void startTrace_refusesATableThatDoesNotCompileInTheProject() {
        givenTable(null, method);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.\nExpected value of type 'Integer'.")));

        var refused = assertThrows(ConflictException.class, () -> startTrace(null));

        assertEquals(ERROR_CODE, refused.getErrorCode());
        assertArrayEquals(new Object[]{"Cannot parse cell value 'abc'."}, refused.getArgs());
        assertNothingStarted();
    }

    @Test
    void startTrace_refusesATableThatDoesNotCompileInItsModule() {
        givenTable(MODULE, method);
        when(projectModel.getOpenedModuleMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.")));

        var refused = assertThrows(ConflictException.class, () -> startTrace(MODULE));

        assertEquals(ERROR_CODE, refused.getErrorCode());
        verify(projectModel, never()).getMessagesByTsn(any(), any());
        assertNothingStarted();
    }

    @Test
    void startTrace_refusesATableWhoseHeaderDoesNotCompile() {
        givenTable(null, null);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR)).thenReturn(List.of(error("Need to close '('")));

        var refused = assertThrows(ConflictException.class, () -> startTrace(null));

        assertEquals(ERROR_CODE, refused.getErrorCode());
        assertNothingStarted();
    }

    @Test
    void startTrace_refusesATableWhoseErrorHasNoSummary() {
        givenTable(null, method);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(new OpenLMessage(null, Severity.ERROR)));

        var refused = assertThrows(ConflictException.class, () -> startTrace(null));

        assertArrayEquals(new Object[]{""}, refused.getArgs());
        assertNothingStarted();
    }

    @Test
    void startTrace_answersNotFoundForATableWithoutAMethodAndWithoutErrors() {
        givenTable(null, null);

        assertThrows(NotFoundException.class, () -> startTrace(null));

        assertNothingStarted();
    }

    @Test
    void startTrace_answersNotFoundForAnUnknownTable() {
        givenProject(null);

        assertThrows(NotFoundException.class, () -> startTrace(null));

        assertNothingStarted();
    }

    @Test
    void startTrace_tracesATableThatCompiles() {
        givenTable(null, method);

        assertTraceStarts(null);
    }

    @Test
    void startTrace_tracesWithinTheModuleATableThatHasErrorsOnlyInTheWholeProject() {
        givenTable(MODULE, method);
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR))
                .thenReturn(List.of(error("There can be only one active table")));

        assertTraceStarts(MODULE);

        verify(projectModel, never()).getMessagesByTsn(any(), any());
    }

    @Test
    void startTrace_refusesATestTableWhoseTestedTableDoesNotCompile() {
        givenTestTable(null, methodWrittenAt(TARGET_URI));
        when(projectModel.getMessagesByTsn(TARGET_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.\nExpected value of type 'Integer'.")));

        var refused = assertThrows(ConflictException.class, () -> startTrace(null));

        assertEquals(ERROR_CODE, refused.getErrorCode());
        assertArrayEquals(new Object[]{"Cannot parse cell value 'abc'."}, refused.getArgs());
        assertNothingStarted();
    }

    @Test
    void startTrace_refusesATestTableWhoseTestedTableDoesNotCompileInItsModule() {
        givenTestTable(MODULE, methodWrittenAt(TARGET_URI));
        when(projectModel.getOpenedModuleMessagesByTsn(TARGET_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.")));

        var refused = assertThrows(ConflictException.class, () -> startTrace(MODULE));

        assertEquals(ERROR_CODE, refused.getErrorCode());
        verify(projectModel, never()).getMessagesByTsn(any(), any());
        assertNothingStarted();
    }

    @Test
    void startTrace_refusesATestTableForAnyOverloadedVersionOfTheTestedTable() {
        var candidates = List.of(methodWrittenAt(TARGET_URI), methodWrittenAt(OTHER_TARGET_URI));
        var dispatcher = mock(OpenMethodDispatcher.class);
        when(dispatcher.getCandidates()).thenReturn(candidates);
        givenTestTable(null, dispatcher);
        when(projectModel.getMessagesByTsn(OTHER_TARGET_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.")));

        assertThrows(ConflictException.class, () -> startTrace(null));

        assertNothingStarted();
    }

    @Test
    void startTrace_refusesATestTableThatDoesNotCompileItself() {
        givenTestTable(null, methodWrittenAt(TARGET_URI));
        when(projectModel.getMessagesByTsn(TABLE_URI, Severity.ERROR)).thenReturn(List.of(error("Bad test header")));

        assertThrows(ConflictException.class, () -> startTrace(null));

        verify(projectModel, never()).getMessagesByTsn(eq(TARGET_URI), any());
        assertNothingStarted();
    }

    @Test
    void startTrace_tracesWithinTheModuleATestTableWhoseTestedTableHasErrorsOnlyInTheWholeProject() {
        givenTestTable(MODULE, methodWrittenAt(TARGET_URI));
        when(projectModel.getMessagesByTsn(TARGET_URI, Severity.ERROR))
                .thenReturn(List.of(error("There can be only one active table")));

        assertTraceStarts(MODULE);

        verify(projectModel, never()).getMessagesByTsn(any(), any());
    }

    @Test
    void startTrace_tracesATestTableWhoseTestedTableCompiles() {
        givenTestTable(null, methodWrittenAt(TARGET_URI));

        assertTraceStarts(null);
    }

    @Test
    void startTrace_ignoresATestedTableThatIsNotInTheProjectAnyMore() {
        givenTestTable(null, methodWrittenAt(TARGET_URI), false);
        when(projectModel.getMessagesByTsn(TARGET_URI, Severity.ERROR))
                .thenReturn(List.of(error("Cannot parse cell value 'abc'.")));

        assertTraceStarts(null);

        verify(projectModel, never()).getMessagesByTsn(eq(TARGET_URI), any());
    }

    @Test
    void startTrace_decidesATestTableByItsMethodNotByItsType() {
        givenTable(null, method);
        when(table.getType()).thenReturn(XlsNodeTypes.XLS_TEST_METHOD.toString());

        assertTraceStarts(null);

        verify(projectModel, never()).getTableByUri(any());
    }

    private void startTrace(String fromModule) {
        controller.startTrace(project, TABLE_ID, null, fromModule, true, false, false, true, true, 20,
                StackViewMode.FULL, false, INPUT);
    }

    private void assertTraceStarts(String fromModule) {
        assertSame(sessionStarted, assertThrows(IllegalStateException.class, () -> startTrace(fromModule)));
        verify(traceDebugService).startSession(any());
    }

    private void assertNothingStarted() {
        verify(traceDebugService, never()).startSession(any());
    }

    private void givenProject(String fromModule) {
        var handle = mock(ProjectHandle.class);
        when(projectService.openProject(project, fromModule)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(projectModel);
    }

    private void givenTable(String fromModule, IOpenMethod tableMethod) {
        givenProject(fromModule);
        when(projectModel.getTableById(TABLE_ID)).thenReturn(table);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(fromModule == null ? tableMethod : null);
        when(projectModel.getOpenedModuleMethod(TABLE_URI)).thenReturn(fromModule == null ? null : tableMethod);
    }

    private void givenTestTable(String fromModule, IOpenMethod testedMethod) {
        givenTestTable(fromModule, testedMethod, true);
    }

    private void givenTestTable(String fromModule, IOpenMethod testedMethod, boolean testedTableIsInTheProject) {
        var suite = mock(TestSuiteMethod.class);
        when(suite.getTestedMethod()).thenReturn(testedMethod);
        givenTable(fromModule, suite);
        when(table.getType()).thenReturn(XlsNodeTypes.XLS_TEST_METHOD.toString());
        if (testedTableIsInTheProject) {
            var testedTable = mock(TableSyntaxNode.class);
            when(projectModel.getTableByUri(any())).thenReturn(testedTable);
        }
    }

    private static IOpenMethod methodWrittenAt(String uri) {
        var node = mock(TableSyntaxNode.class);
        when(node.getUri()).thenReturn(uri);
        when(node.getId()).thenReturn(uri);
        var info = mock(IMemberMetaInfo.class);
        when(info.getSyntaxNode()).thenReturn(node);
        var written = mock(IOpenMethod.class);
        when(written.getInfo()).thenReturn(info);
        return written;
    }

    private static OpenLMessage error(String summary) {
        return new OpenLMessage(summary, Severity.ERROR);
    }
}
