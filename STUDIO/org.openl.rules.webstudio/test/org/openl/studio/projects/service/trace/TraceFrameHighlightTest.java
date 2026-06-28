package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.rules.webstudio.web.trace.debug.DebugCommand;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.rules.webstudio.web.trace.debug.TraceDebugger;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Confirms the traced table highlights the current spreadsheet cell while suspended.
 */
class TraceFrameHighlightTest {

    private static final String PROJECT = "test/rules/EPBDS-16160";

    @Test
    void currentCellIsHighlightedInTheRenderedTable() throws Exception {
        ProjectModel projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(ProjectResolver.getInstance().resolve(Path.of(PROJECT)).getModules().getFirst());
        CompiledOpenClass compiled = projectModel.getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClassWithErrors();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("highlight-test", compiled.getClassLoader(), true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));
            var service = new TraceTableHtmlServiceImpl();

            // At the spreadsheet entry there is no current cell, so no highlight is applied.
            DebugFrame frame = debugger.stack().get(debugger.stack().size() - 1);
            String atEntry = service.renderFrameTable(projectModel, frame, false);

            // After stepping onto the first cell, that cell must be highlighted (render differs).
            assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, 10_000));
            frame = debugger.stack().get(debugger.stack().size() - 1);
            assertNotNull(frame.getLocation());
            assertEquals("cell", frame.getLocation().kind());
            String atCell = service.renderFrameTable(projectModel, frame, false);

            assertNotEquals(atEntry, atCell, "the current cell must be highlighted in the table");
        } finally {
            debugger.terminate(10_000);
        }
    }
}
