package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.studio.projects.model.trace.FrameKind;

class DebugSessionRegistryTest {

    private static ProjectId projectId(String name) {
        return ProjectIds.of("repo", name);
    }

    private static DebugSession session(ProjectId projectId) {
        return new DebugSession(projectId, "table", new TraceDebugger(DebugListener.NOOP), null, null, Map.of(),
                "session-1");
    }

    private static DebugSessionRegistry registry() {
        return new DebugSessionRegistry(new DebugSessionReaper());
    }

    @Test
    void startReplacesCurrentSession() {
        var registry = registry();
        var first = session(projectId("A"));
        registry.start(first);
        assertSame(first, registry.find(projectId("A")));

        var second = session(projectId("A"));
        registry.start(second);
        assertSame(second, registry.find(projectId("A")));
    }

    @Test
    void findMatchesOnlyTheOwningProject() {
        var registry = registry();
        registry.start(session(projectId("A")));
        assertNotNull(registry.find(projectId("A")));
        assertNull(registry.find(projectId("B")));
    }

    @Test
    void clearDropsTheSession() {
        var registry = registry();
        registry.start(session(projectId("A")));
        registry.clear();
        assertNull(registry.find(projectId("A")));
    }

    @Test
    void breakpointsPersistAndApplyToTheRunningSession() {
        var registry = registry();
        var session = session(projectId("A"));
        registry.start(session);

        registry.setBreakpoints(Set.of("uri/1", "uri/2"));
        assertEquals(Set.of("uri/1", "uri/2"), registry.breakpoints());
        assertEquals(Set.of("uri/1", "uri/2"), session.getDebugger().getBreakpoints());
    }

    @Test
    void watchesPersistAndApplyToTheRunningSession() {
        var registry = registry();
        var session = session(projectId("A"));
        registry.start(session);

        registry.setWatches(Set.of("$A", "$B"));
        assertEquals(Set.of("$A", "$B"), registry.watches());
        assertEquals(Set.of("$A", "$B"), session.getDebugger().getWatches(),
                "a watch added mid-debug applies to the running session");
    }

    @Test
    void workspaceResetClearsTheSession() {
        var registry = registry();
        registry.start(session(projectId("A")));
        registry.onWorkspaceReset(new WorkspaceResetEvent(this));
        assertNull(registry.find(projectId("A")));
    }

    @Test
    void remembersLaunchInputAcrossAClearSoARestartCanReuseIt() {
        var registry = registry();
        assertNull(registry.lastInputJson());

        registry.rememberInputJson("{\"policy\":1}");
        registry.clear();

        // A restart (profiling toggle, replay) cancels then re-launches; the input must survive the cancel.
        assertEquals("{\"policy\":1}", registry.lastInputJson());
    }

    private static CallNode tree() {
        return new CallNode("u", "T", 0, FrameKind.SPREADSHEET, 1, List.of(), null, null);
    }

    @Test
    void keptTreeOutlivesTheSessionAndAnswersOnlyItsOwnRun() {
        var registry = registry();
        var tree = tree();
        registry.start(session(projectId("A")));
        registry.keepTree(new TraceTreeSnapshot("run-1", projectId("A"), tree, 1));

        // A click on a row cancels the session and starts another; the tree of the run stays.
        registry.clear();
        registry.start(session(projectId("A")));

        assertSame(tree, registry.tree(projectId("A"), "run-1"));
        assertNull(registry.tree(projectId("A"), "run-0"), "a branch of an earlier run is not found");
        assertNull(registry.tree(projectId("B"), "run-1"), "nor a branch of another project");
    }

    @Test
    void keepsATreeForEveryRunSoEveryWindowBrowsesItsOwn() {
        var registry = registry();
        var first = new TraceTreeSnapshot("run-1", projectId("A"), tree(), 1);
        registry.keepTree(first);
        registry.keepTree(new TraceTreeSnapshot("run-2", projectId("A"), tree(), 1));

        assertNotNull(registry.tree(projectId("A"), "run-1"), "a run in another window keeps the first tree");
        assertNotNull(registry.tree(projectId("A"), "run-2"));

        registry.releaseTree("run-1");
        assertNull(registry.tree(projectId("A"), "run-1"));
        assertNull(first.tree(projectId("A")), "the released tree is freed, not only forgotten");
        assertNotNull(registry.tree(projectId("A"), "run-2"), "releasing one run leaves the others");
    }

    @Test
    void releaseOfEveryTreeAndWorkspaceResetDropTheKeptTrees() {
        var registry = registry();
        registry.keepTree(new TraceTreeSnapshot("run-1", projectId("A"), tree(), 1));
        registry.keepTree(new TraceTreeSnapshot("run-2", projectId("A"), tree(), 1));
        registry.releaseTrees();
        assertNull(registry.tree(projectId("A"), "run-1"));
        assertNull(registry.tree(projectId("A"), "run-2"));

        registry.keepTree(new TraceTreeSnapshot("run-3", projectId("A"), tree(), 1));
        registry.onWorkspaceReset(new WorkspaceResetEvent(this));
        assertNull(registry.tree(projectId("A"), "run-3"));
    }

    @Test
    void aTraceRequestOfTheClientKeepsItsTreesFromIdling() {
        var registry = registry();
        var kept = spy(new TraceTreeSnapshot("run-1", projectId("A"), tree(), 1));
        registry.keepTree(kept);
        clearInvocations(kept);

        // A click on a row restarts the session and reads its stack: no branch is read, yet the window is in use.
        registry.start(session(projectId("A")));
        registry.find(projectId("A"));

        verify(kept, times(2)).touch();
    }
}
