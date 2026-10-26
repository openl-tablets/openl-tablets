package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketDebugListenerFactory;
import org.openl.studio.projects.model.trace.CallNodeView;
import org.openl.studio.projects.model.trace.DebugStackView;
import org.openl.studio.projects.model.trace.DebugStatus;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.studio.projects.model.trace.StackViewMode;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.trace.CallNode;
import org.openl.studio.projects.service.trace.DebugSession;
import org.openl.studio.projects.service.trace.DebugSessionReaper;
import org.openl.studio.projects.service.trace.DebugSessionRegistry;
import org.openl.studio.projects.service.trace.TraceDebugService;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.studio.projects.service.trace.TraceExportService;
import org.openl.studio.projects.service.trace.TraceHighlightService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenMethod;

/** The executed tree a business-view run keeps for its deep branches, read through the trace endpoints. */
class ProjectsTraceDebugControllerTreeTest {

    private static final String TABLE_ID = "abc123";
    private static final String TABLE_URI = "file:/Test/rules/Main.xlsx?sheet=Rules&range=B3:E11";

    private final RulesProject project = mock(RulesProject.class);
    private final ProjectId projectId = ProjectIds.of("design", "Test");
    private final WorkspaceProjectService projectService = mock(WorkspaceProjectService.class);
    private final TraceDebugService traceDebugService = mock(TraceDebugService.class);
    private final DebugSessionRegistry sessionRegistry = new DebugSessionRegistry(new DebugSessionReaper());
    private ProjectsTraceDebugController controller;

    @BeforeEach
    void init() {
        when(project.getProjectId()).thenReturn(projectId);
        var workspace = mock(UserWorkspace.class);
        when(projectService.getUserWorkspace()).thenReturn(workspace);
        var handle = mock(ProjectHandle.class);
        var projectModel = mock(ProjectModel.class);
        var table = mock(IOpenLTable.class);
        var method = mock(IOpenMethod.class);
        when(projectService.openProject(project, null)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(projectModel);
        when(projectModel.getTableById(TABLE_ID)).thenReturn(table);
        when(table.getUri()).thenReturn(TABLE_URI);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        controller = new ProjectsTraceDebugController(
                projectService,
                traceDebugService,
                sessionRegistry,
                mock(SocketDebugListenerFactory.class),
                mock(TraceParameterRegistry.class),
                mock(TraceHighlightService.class),
                mock(TraceExportService.class),
                mock(ProjectTablesGraphService.class),
                mock(ProjectObjectMapperService.class)) {
            @Override
            protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
                return mock(SchemaGenerator.class);
            }
        };
    }

    @Test
    void aBusinessRunKeepsItsTreeAfterTheClicksReplaceItsSession() {
        startBusinessRun("run-1", tree());

        // A click on a row cancels the session, which leaves the tree, and starts one that records none.
        controller.cancelTrace(project);
        startClickRun("run-2");

        assertEquals(List.of("B", "C"), callNames("run-1"));
    }

    @Test
    void aBusinessRunInAnotherWindowLeavesTheTreeOfTheFirst() {
        startBusinessRun("run-1", tree());

        startBusinessRun("run-2", tree());

        assertEquals(List.of("B", "C"), callNames("run-1"));
        assertEquals(List.of("B", "C"), callNames("run-2"));
    }

    @Test
    void aWindowReleasesOnlyTheTreeOfItsRun() {
        startBusinessRun("run-1", tree());
        startBusinessRun("run-2", tree());

        controller.releaseTree(project, "run-1");

        assertReleased("run-1");
        assertEquals(List.of("B", "C"), callNames("run-2"));
    }

    @Test
    void theCallsOfAnotherRunAreNotFound() {
        startBusinessRun("run-1", tree());

        assertReleased("run-0");
        assertEquals(List.of("B", "C"), callNames("run-1"));
    }

    @Test
    void theStartOfABusinessRunListsTheTreeLevelByLevel() {
        var stack = startBusinessRun("run-1", tree());

        assertEquals(List.of("A", "B", "C"), stack.treeNodes().stream().map(CallNodeView::name).toList());
        assertNull(stack.tree());
    }

    private DebugStackView startBusinessRun(String runId, CallNode completed) {
        givenSession(runId, completed);
        return controller.startTrace(project, TABLE_ID, null, null, false, true, true, false, true, 20,
                StackViewMode.FULL, true, null);
    }

    private void startClickRun(String runId) {
        givenSession(runId, null);
        controller.startTrace(project, TABLE_ID, null, null, true, false, false, true, true, 20,
                StackViewMode.FULL, false, null);
    }

    private void givenSession(String runId, CallNode completed) {
        var debugger = mock(TraceDebugger.class);
        when(debugger.status()).thenReturn(DebugStatus.COMPLETED);
        when(debugger.completedTree()).thenReturn(completed);
        when(debugger.recordedNodes()).thenReturn(3);
        var session = new DebugSession(projectId, TABLE_ID, debugger, null, null, Map.of(), runId);
        when(traceDebugService.startSession(any())).thenReturn(session);
    }

    private List<String> callNames(String runId) {
        return controller.treeNode(project, runId, "uA", 0, "R0C0", 0).nodes().stream()
                .map(CallNodeView::name)
                .toList();
    }

    private void assertReleased(String runId) {
        var refused = assertThrows(NotFoundException.class,
                () -> controller.treeNode(project, runId, "uA", 0, "R0C0", 0));
        assertEquals("openl.error.404.trace.tree.released.message", refused.getErrorCode());
    }

    /** Root A, whose step calls B and C. */
    private static CallNode tree() {
        return new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, 2,
                List.of(new CallNode.Step("R0C0", "$s", 2, List.of(leaf("uB", "B"), leaf("uC", "C")))), null, null);
    }

    private static CallNode leaf(String uri, String name) {
        return new CallNode(uri, name, 0, FrameKind.SPREADSHEET, 1, List.of(), null, null);
    }
}
