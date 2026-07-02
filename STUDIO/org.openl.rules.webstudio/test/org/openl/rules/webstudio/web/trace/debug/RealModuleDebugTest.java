package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * End-to-end engine test against a real compiled module: drives the genuine OpenL execution through
 * the debug tracer and the production {@link DefaultSourceClassifier}.
 */
class RealModuleDebugTest {

    private static final String SRC = "test/rules/EPBDS-16160/generalProject.xlsx";

    @Test
    @DisplayName("Suspends inside a real spreadsheet and resumes to completion")
    void debugsRealSpreadsheet() {
        CompiledOpenClass compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClass();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("test-real", compiled.getClassLoader(), true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            Object target = module.newInstance(env);
            myRule.invoke(target, new Object[0], env);
        });

        // Stopped at the entry of the spreadsheet frame.
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));
        List<DebugFrame> stack = debugger.stack();
        assertFalse(stack.isEmpty(), "stack must not be empty when suspended");
        DebugFrame top = stack.get(stack.size() - 1);
        assertEquals(FrameKind.SPREADSHEET, top.getKind(), "MyRule is a spreadsheet");
        assertNotNull(top.getUri(), "frame must carry a table URI");
        assertEquals("MyRule", top.getName());

        // Stepping into the spreadsheet lands on a cell sub-step in the same frame.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, 10_000));
        DebugFrame current = debugger.stack().get(debugger.stack().size() - 1);
        assertEquals("MyRule", current.getName());
        assertNotNull(current.getLocation(), "stepping into a spreadsheet exposes a current cell");
        assertEquals(LocationKind.CELL, current.getLocation().kind());

        // Resume runs the rest of the spreadsheet to completion.
        assertEquals(DebugStatus.COMPLETED, debugger.command(DebugCommand.RESUME, 10_000));
    }
}
