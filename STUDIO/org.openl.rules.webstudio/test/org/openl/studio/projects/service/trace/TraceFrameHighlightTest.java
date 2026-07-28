package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.CellHighlight;
import org.openl.studio.projects.model.trace.DebugStatus;
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
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(ProjectResolver.getInstance().resolve(Path.of(PROJECT)).getModules().getFirst());
        var compiled = projectModel.getCompiledOpenClass();
        var module = compiled.getOpenClassWithErrors();
        var myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("highlight-overlay-test", compiled.getClassLoader(), true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));
            var service = new TraceHighlightServiceImpl();

            // Step onto the first cell so there is a current line to overlay.
            assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, 10_000));
            var frame = debugger.stack().get(debugger.stack().size() - 1);

            var highlights = service.computeHighlights(frame);
            assertFalse(highlights.isEmpty(), "the current cell must be exposed as a highlight");
            var current = highlights.getFirst();
            assertEquals(HighlightState.CURRENT, current.state());
            assertTrue(current.cell().matches("[A-Z]+\\d+"),
                    "the highlight must be keyed by an A1 cell address, was: " + current.cell());
        } finally {
            debugger.terminate(10_000);
        }
    }

    @Test
    void aFiredDecisionTableRuleAccentsItsWholeRowAndItsResult() throws Exception {
        // The bank-rating module calls the NonZeroValues decision table.
        CompiledOpenClass compiled = new RulesEngineFactory<>("test/rules/EPBDS-16292/bankRating.xlsx")
                .getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClass();
        IOpenMethod test = module.getMethod("BankRatingTest", IOpenClass.EMPTY);
        assertNotNull(test, "BankRatingTest must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setBreakpoints(Set.of("NonZeroValues")); // stop at the decision table
        debugger.start("dt-highlight-test", compiled.getClassLoader(), false, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            test.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(20_000));
            // Run the table to its own exit, where the rule has fired and the result is on the frame.
            assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, 20_000));
            DebugFrame dt = debugger.stack().get(debugger.stack().size() - 1);
            assertTrue(dt.getSource() instanceof IDecisionTable, "the frame is the decision table");
            assertFalse(dt.getFiredRules().isEmpty(), "a rule fired — recorded independently of the current step");

            List<CellHighlight> highlights = new TraceHighlightServiceImpl().computeHighlights(dt);
            // The fired rule is accented across its whole row/column (result included), not just one corner cell.
            long resultCells = highlights.stream().filter(h -> h.state() == HighlightState.RESULT).count();
            assertTrue(resultCells > 1, "the fired rule spans several accented cells: " + highlights);
            // Evaluated conditions are marked too.
            assertTrue(highlights.stream().anyMatch(h -> h.state() == HighlightState.CONDITION_TRUE
                            || h.state() == HighlightState.CONDITION_FALSE),
                    "evaluated conditions are marked: " + highlights);
        } finally {
            debugger.terminate(20_000);
        }
    }
}
