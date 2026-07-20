package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Verifies the retained call tree when profiling hits the node cap while a nested call is still running.
 *
 * <p>The module is three levels deep: {@code Root} calls {@code Child}, and {@code Child}'s cells each
 * re-read a seed, leaving reference nodes under it. A called frame reserves its tree slot when it starts,
 * so it stays attached even when its own descendants exhaust the cap first. Otherwise the whole nested
 * call — together with the descendants already recorded under it — would vanish from the tree and its
 * time would be misattributed to the caller.
 */
class TreeNodeCapTest {

    private static final String SRC = "test/rules/trace-debug/nestedCallProject.xlsx";

    @Test
    @DisplayName("Keeps a nested call in the tree even when its own subtree exhausts the node cap")
    void retainsNestedCallWhoseSubtreeFillsTheCap() {
        // Root(1) + Child(2) + one reference(3) reach the cap, so Child's remaining references are truncated
        // while Child is still running — the point at which the caller-side cap check used to drop Child.
        TraceDebugger debugger = trace(3);

        CallNode tree = debugger.completedTree();
        assertNotNull(tree, "profiling keeps the executed tree");
        assertEquals("Root", tree.name());

        CallNode child = childOf(tree);
        assertNotNull(child, "the nested Child call must stay in the tree, not be dropped with its subtree");
        assertEquals(FrameKind.SPREADSHEET, child.kind());

        assertTrue(debugger.isTreeTruncated(), "hitting the cap flags the tree truncated");
        assertEquals(1, childReferences(child),
                "only the references past the cap are dropped, never the nested call itself");
    }

    @Test
    @DisplayName("Keeps the whole nested call tree when it stays under the cap")
    void keepsFullTreeUnderTheCap() {
        TraceDebugger debugger = trace(1000);

        CallNode child = childOf(debugger.completedTree());
        assertNotNull(child, "the nested Child call is retained");
        assertFalse(debugger.isTreeTruncated(), "a tree under the cap is not truncated");
        assertEquals(3, childReferences(child), "under the cap every reference is retained");
    }

    private TraceDebugger trace(int cap) {
        CompiledOpenClass compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> "module must compile: " + compiled.getAllMessages());
        IOpenClass module = compiled.getOpenClass();
        IOpenMethod root = module.getMethods().stream()
                .filter(method -> "Root".equals(method.getName()))
                .findFirst()
                .orElseThrow();

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setMaxTreeNodes(cap);
        debugger.start("node-cap", compiled.getClassLoader(), false, true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            root.invoke(module.newInstance(env), new Object[0], env);
        });
        assertNotNull(debugger.awaitInitialHalt(15_000));
        return debugger;
    }

    /** The nested {@code Child} spreadsheet frame, or {@code null} if it was dropped from the tree. */
    private static CallNode childOf(CallNode tree) {
        for (CallNode.Step step : tree.steps()) {
            for (CallNode call : step.children()) {
                if (call.kind() != FrameKind.STEP_REF && "Child".equals(call.name())) {
                    return call;
                }
            }
        }
        return null;
    }

    /** How many reference nodes were retained under the given frame's steps. */
    private static int childReferences(CallNode frame) {
        return frame.steps().stream().mapToInt(step -> step.children().size()).sum();
    }
}
