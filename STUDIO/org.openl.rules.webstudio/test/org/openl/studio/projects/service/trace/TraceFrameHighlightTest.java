package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

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
import org.openl.studio.projects.model.trace.CellHighlight;
import org.openl.studio.projects.model.trace.HighlightState;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Confirms the trace exposes the current spreadsheet cell as an A1-keyed highlight overlay while suspended.
 */
class TraceFrameHighlightTest {

    private static final String PROJECT = "test/rules/EPBDS-16160";

    @Test
    void currentCellIsExposedAsAnA1KeyedOverlay() throws Exception {
        ProjectModel projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(ProjectResolver.getInstance().resolve(Path.of(PROJECT)).getModules().getFirst());
        CompiledOpenClass compiled = projectModel.getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClassWithErrors();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("highlight-overlay-test", compiled.getClassLoader(), true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));
            var service = new TraceHighlightServiceImpl();

            // Step onto the first cell so there is a current line to overlay.
            assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, 10_000));
            DebugFrame frame = debugger.stack().get(debugger.stack().size() - 1);

            List<CellHighlight> highlights = service.computeHighlights(frame);
            assertFalse(highlights.isEmpty(), "the current cell must be exposed as a highlight");
            CellHighlight current = highlights.getFirst();
            assertEquals(HighlightState.CURRENT, current.state());
            assertTrue(current.cell().matches("[A-Z]+\\d+"),
                    "the highlight must be keyed by an A1 cell address, was: " + current.cell());
        } finally {
            debugger.terminate(10_000);
        }
    }
}
