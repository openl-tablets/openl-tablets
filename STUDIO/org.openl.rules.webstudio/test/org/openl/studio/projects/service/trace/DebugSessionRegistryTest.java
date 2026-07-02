package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.rules.webstudio.web.trace.debug.TraceDebugger;
import org.openl.studio.projects.model.ProjectIdModel;

class DebugSessionRegistryTest {

    private static ProjectIdModel projectId(String name) {
        return ProjectIdModel.builder().repository("repo").projectName(name).build();
    }

    private static DebugSession session(ProjectIdModel projectId) {
        return new DebugSession(projectId, "table", new TraceDebugger(DebugListener.NOOP), null);
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
    void workspaceResetClearsTheSession() {
        var registry = registry();
        registry.start(session(projectId("A")));
        registry.onWorkspaceReset(new WorkspaceResetEvent(this));
        assertNull(registry.find(projectId("A")));
    }
}
