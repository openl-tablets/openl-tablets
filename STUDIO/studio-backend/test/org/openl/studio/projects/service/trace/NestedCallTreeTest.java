package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.FrameKind;

/**
 * Verifies the executed call tree of a nested call.
 *
 * <p>The module is three levels deep: {@code Root} calls {@code Child}, and {@code Child}'s cells each
 * re-read a seed, leaving reference nodes under it.
 */
class NestedCallTreeTest {

    private static final String SRC = "test/rules/trace-debug/nestedCallProject.xlsx";

    @Test
    @DisplayName("Keeps the nested call and every reference under it")
    void keepsTheWholeNestedCallTree() {
        var tree = trace();
        assertEquals("Root", tree.name());

        var child = childCall(stepCallingChild(tree));
        assertEquals(FrameKind.SPREADSHEET, child.kind());
        assertEquals(3, childReferences(child), "every re-read of the seed is kept");
    }

    @Test
    @DisplayName("A step's total time includes the nested call it makes")
    void stepTotalIncludesItsNestedCall() {
        var tree = trace();
        var caller = stepCallingChild(tree);
        var child = childCall(caller);
        // Total time flows outward: a step that calls Child owns Child's whole duration. Subtracting nested-step
        // time must stop at the frame boundary, or the caller would drop the callee's own steps and read as
        // faster than the very call it makes.
        assertTrue(caller.durationNanos() >= child.durationNanos(),
                () -> "step total " + caller.durationNanos() + "ns must include its nested call "
                        + child.durationNanos() + "ns");
    }

    private static CallNode trace() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> "module must compile: " + compiled.getAllMessages());
        var module = compiled.getOpenClass();
        var root = module.getMethods().stream()
                .filter(method -> "Root".equals(method.getName()))
                .findFirst()
                .orElseThrow();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("nested-call", compiled.getClassLoader(), false, true, false, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            root.invoke(module.newInstance(env), new Object[0], env);
        });
        assertNotNull(debugger.awaitInitialHalt(15_000));
        var tree = debugger.completedTree();
        assertNotNull(tree, "profiling keeps the executed tree");
        return tree;
    }

    /** The step of {@code Root} that calls {@code Child}; fails when the nested call is not kept. */
    private static CallNode.Step stepCallingChild(CallNode tree) {
        return tree.steps().stream()
                .filter(step -> step.children().stream().anyMatch(NestedCallTreeTest::isChildCall))
                .findFirst()
                .orElseThrow();
    }

    /** The {@code Child} call the given step makes. */
    private static CallNode childCall(CallNode.Step step) {
        return step.children().stream()
                .filter(NestedCallTreeTest::isChildCall)
                .findFirst()
                .orElseThrow();
    }

    private static boolean isChildCall(CallNode call) {
        return call.kind() != FrameKind.STEP_REF && "Child".equals(call.name());
    }

    /** How many reference nodes are kept under the given frame's steps. */
    private static int childReferences(CallNode frame) {
        return frame.steps().stream().mapToInt(step -> step.children().size()).sum();
    }
}
