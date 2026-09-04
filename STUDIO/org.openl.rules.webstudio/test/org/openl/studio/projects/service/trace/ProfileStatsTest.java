package org.openl.studio.projects.service.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.studio.projects.model.trace.TableProfile;

/**
 * Verifies that the profiling hotspots are aggregated on the fly, per table, independent of the retained
 * call tree. The module's {@code Root} calls {@code SubCalc} three times over an array, so the stats must
 * count all three invocations — even when the node cap truncates the tree so only one is kept for display.
 */
class ProfileStatsTest {

    private static final String SRC = "test/rules/trace-debug/arrayCallProject.xlsx";

    @Test
    @DisplayName("Counts every table invocation even when the tree is truncated for display")
    void countsEveryInvocationWhenTheTreeTruncates() {
        // Cap the tree so only the first SubCalc survives, then confirm the stats still counted all three.
        var debugger = trace(2);

        assertTrue(debugger.isTreeTruncated(), "the low node cap truncates the tree");
        assertTrue(subCalcNodesInTree(debugger.completedTree()) < 3,
                "the display tree drops the invocations past the cap");
        assertEquals(3, stat(debugger.profileStats(), "SubCalc").count(),
                "the hotspots count every invocation, not just the ones kept in the tree");
        assertTrue(debugger.completedTree().notRetained() > 0,
                "the dropped SubCalc frames are counted as not-retained sub-calls on Root, so the gap is visible");
    }

    @Test
    @DisplayName("Aggregates self and total time per table")
    void aggregatesSelfAndTotalPerTable() {
        var debugger = trace(1000);

        TableProfile subCalc = stat(debugger.profileStats(), "SubCalc");
        assertEquals(3, subCalc.count());
        assertEquals(subCalc.totalNanos(), subCalc.selfNanos(),
                "SubCalc calls no other table, so its own time is its whole time");

        TableProfile root = stat(debugger.profileStats(), "Root");
        assertEquals(1, root.count());
        assertTrue(root.selfNanos() < root.totalNanos(),
                "Root's own time excludes the SubCalc invocations it made");
    }

    @Test
    @DisplayName("Shares one step-ref instance across a table's repeated invocations")
    void sharesRepeatedStepStringsAcrossInvocations() {
        var debugger = trace(1000);

        var subCalls = new ArrayList<CallNode>();
        walk(debugger.completedTree(), node -> {
            if (node.kind() != FrameKind.STEP_REF && "SubCalc".equals(node.name())) {
                subCalls.add(node);
            }
        });

        assertEquals(3, subCalls.size(), "SubCalc runs three times");
        // The step refs are rebuilt on every execution, so three invocations would keep three copies of each.
        // Interning collapses them to one shared instance — the memory win, since a table can run thousands of times.
        var ref0 = subCalls.getFirst().steps().getFirst().ref();
        assertSame(ref0, subCalls.get(1).steps().getFirst().ref(), "repeated invocations share one step-ref instance");
        assertSame(ref0, subCalls.get(2).steps().getFirst().ref());
    }

    private TraceDebugger trace(int cap) {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> "module must compile: " + compiled.getAllMessages());
        var module = compiled.getOpenClass();
        var root = module.getMethods().stream()
                .filter(method -> "Root".equals(method.getName()))
                .findFirst()
                .orElseThrow();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setMaxTreeNodes(cap);
        debugger.start("profile-stats", compiled.getClassLoader(), false, true, false, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            root.invoke(module.newInstance(env), new Object[0], env);
        });
        assertNotNull(debugger.awaitInitialHalt(15_000));
        return debugger;
    }

    private static TableProfile stat(List<TableProfile> stats, String name) {
        return stats.stream()
                .filter(stat -> name.equals(stat.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stats for " + name + " in "
                        + stats.stream().map(TableProfile::name).toList()));
    }

    private static int subCalcNodesInTree(CallNode tree) {
        int[] count = {0};
        walk(tree, node -> {
            if (node.kind() != FrameKind.STEP_REF && "SubCalc".equals(node.name())) {
                count[0]++;
            }
        });
        return count[0];
    }

    private static void walk(CallNode node, java.util.function.Consumer<CallNode> visitor) {
        if (node == null) {
            return;
        }
        for (CallNode.Step step : node.steps()) {
            for (CallNode child : step.children()) {
                visitor.accept(child);
                walk(child, visitor);
            }
        }
    }
}
