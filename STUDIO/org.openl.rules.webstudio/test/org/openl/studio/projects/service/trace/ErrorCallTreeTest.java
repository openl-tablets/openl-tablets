package org.openl.studio.projects.service.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.DebugStatus;

/**
 * The business full run debugs with break-on-exception off (see {@link TraceDebugger#setBreakOnErrors}), so a
 * rule error ends the run and the executed tree — with the failed branch — is kept for offline browsing.
 *
 * <p>Fixture {@code errorProject.xlsx}: {@code DeterminePolicy} calls {@code DetermineManual}, which calls
 * {@code Validate}, whose {@code Result} cell raises {@code error("Some text")}. The two calling steps
 * ({@code $ManualRates}, {@code $Validation}) never return — they are interrupted mid-call by the error.
 */
class ErrorCallTreeTest {

    private static final String SRC = "test/rules/trace-debug/errorProject.xlsx";

    @Test
    @DisplayName("A step interrupted by an error in the table it called keeps its own name, not a raw RnCm ref")
    void keepsRealStepNamesOnTheFailedBranch() {
        var tree = runToError();

        // The step that called the failing table never returned, so it is recorded only by its sub-call. It
        // must still read with its own name — the tree used to fall back to the raw RnCm ref (e.g. "R1C0").
        CallNode.Step manualRates = step(tree, "$ManualRates");
        assertEquals(List.of("DetermineManual"), childNames(manualRates), "the failed branch hangs under it");

        CallNode.Step validation = step(manualRates.children().getFirst(), "$Validation");
        assertEquals(List.of("Validate"), childNames(validation), "the next level down keeps its name too");

        // No step anywhere on the kept tree is left labelled as its own RnCm reference.
        assertNoRawRefLabels(tree);
    }

    @Test
    @DisplayName("Running through the error keeps the whole executed tree with the failed branch")
    void keepsTheExecutedTreeOnError() {
        var tree = runToError();
        assertEquals("DeterminePolicy", tree.name());
        // Base computed before the error and stays; the failing branch is retained under $ManualRates.
        assertNotNull(step(tree, "$Base"), "steps that finished before the error are kept");
        assertEquals(List.of("Validate"),
                childNames(step(step(tree, "$ManualRates").children().getFirst(), "$Validation")));
    }

    /** Run DeterminePolicy through the error the way the business view does, and return the kept tree. */
    private static CallNode runToError() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> "module must compile: " + compiled.getAllMessages());
        var module = compiled.getOpenClass();
        var method = module.getMethods().stream()
                .filter(candidate -> "DeterminePolicy".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setBreakOnErrors(false);
        debugger.start("error-tree", compiled.getClassLoader(), false, true, false, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            var target = module.newInstance(env);
            method.invoke(target, new Object[]{1}, env);
        });
        assertEquals(DebugStatus.ERROR, debugger.awaitInitialHalt(10_000),
                "the error ends the run instead of parking on it");
        var tree = debugger.completedTree();
        assertNotNull(tree, "the executed tree outlives the failed run");
        return tree;
    }

    /** The executed step with the given label. */
    private static CallNode.Step step(CallNode node, String label) {
        return node.steps().stream()
                .filter(step -> label.equals(step.label()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no step " + label + " in "
                        + node.steps().stream().map(CallNode.Step::label).toList()));
    }

    private static List<String> childNames(CallNode.Step step) {
        return step.children().stream().map(CallNode::name).toList();
    }

    /** No step is labelled as its own {@code RnCm} reference — every one resolved to a real name. */
    private static void assertNoRawRefLabels(CallNode node) {
        for (CallNode.Step step : node.steps()) {
            assertFalse(step.ref().equals(step.label()),
                    () -> "step " + step.ref() + " fell back to its raw reference as a label");
            step.children().forEach(ErrorCallTreeTest::assertNoRawRefLabels);
        }
    }
}
