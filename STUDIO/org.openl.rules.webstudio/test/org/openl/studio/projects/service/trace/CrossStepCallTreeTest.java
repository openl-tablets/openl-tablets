package org.openl.studio.projects.service.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.DebugStatus;
import org.openl.studio.projects.model.trace.FrameKind;

/**
 * Verifies the executed call tree when spreadsheet steps reference each other, on a module shaped like
 * the real premium-calculation rules: the first step's formula resolves a later step, which resolves
 * another later step, which computes yet another step and passes it into a spreadsheet call.
 *
 * <p>A table called from a formula must hang under the step whose formula calls it — not under the step
 * that happened to be computed on the way. Steps computed or re-read by another step's formula appear
 * under it as references, never as duplicated branches.
 */
class CrossStepCallTreeTest {

    private static final String SRC = "test/rules/trace-debug/crossStepProject.xlsx";

    @Test
    @DisplayName("Attributes calls and step references to the step whose formula makes them")
    void attributesCallsToTheCallingStep() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> "module must compile: " + compiled.getAllMessages());
        var module = compiled.getOpenClass();
        var method = module.getMethods().stream()
                .filter(candidate -> "DeterminePremium".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("cross-step", compiled.getClassLoader(), false, true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            var target = module.newInstance(env);
            method.invoke(target, new Object[]{10}, env);
        });
        assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(10_000));

        var tree = debugger.completedTree();
        assertNotNull(tree, "profiling keeps the executed tree");
        assertEquals("DeterminePremium", tree.name());

        // Value_Premiums computed $Term on the way and then called SubPremium: both belong to it.
        CallNode.Step valuePremiums = step(tree, "$Value_Premiums");
        assertEquals(List.of(FrameKind.STEP_REF, FrameKind.SPREADSHEET), kinds(valuePremiums));
        assertEquals("$Term", valuePremiums.children().getFirst().name());
        assertEquals("SubPremium", valuePremiums.children().get(1).name());

        // The step computed on the way carries no children: the call was made by the referring formula.
        assertTrue(step(tree, "$Term").children().isEmpty(),
                "a referenced step must not adopt the referrer's calls");

        // GetContext resolved $RatingDate first, then called SetCtx with it.
        CallNode.Step getContext = step(tree, "$GetContext");
        assertEquals(List.of(FrameKind.STEP_REF, FrameKind.SPREADSHEET), kinds(getContext));
        assertEquals("$RatingDate", getContext.children().getFirst().name());
        assertEquals("SetCtx", getContext.children().get(1).name());

        // RatingDate triggered the Value_Premiums computation from its own formula.
        CallNode.Step ratingDate = step(tree, "$RatingDate");
        assertEquals(List.of(FrameKind.STEP_REF), kinds(ratingDate));
        assertEquals("$Value_Premiums", ratingDate.children().getFirst().name());

        // Re-reads of already-computed steps are references pointing at the original, never new branches.
        CallNode.Step total = step(tree, "$Total");
        assertEquals(List.of(FrameKind.STEP_REF, FrameKind.STEP_REF), kinds(total));
        assertEquals("$RatingDate", total.children().getFirst().name());
        assertEquals("$Term", total.children().get(1).name());
        total.children().forEach(reference -> {
            assertNotNull(reference.refStep(), "a reference carries the ref of the original step");
            assertTrue(reference.steps().isEmpty(), "a reference never duplicates the original's branch");
            assertEquals(0, reference.durationNanos(), "the time is accounted at the original step");
        });
    }

    @Test
    @DisplayName("A step that triggers another cell does not count that cell's time twice")
    void doesNotDoubleCountNestedStepTime() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var method = module.getMethods().stream()
                .filter(candidate -> "DeterminePremium".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("cross-step-timing", compiled.getClassLoader(), false, true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            var target = module.newInstance(env);
            method.invoke(target, new Object[]{10}, env);
        });
        assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(10_000));

        var tree = debugger.completedTree();
        assertNotNull(tree);
        // Several steps resolve or call other cells on the way ($GetContext → $RatingDate → $Value_Premiums →
        // $Term). Each step's own time excludes that nested work, so the steps are disjoint slices of the frame
        // and never sum past its own duration. Before nested time was subtracted, the referring step counted the
        // referenced cell as well and the steps overran the frame.
        var stepsTotal = tree.steps().stream().mapToLong(CallNode.Step::durationNanos).sum();
        assertTrue(stepsTotal <= tree.durationNanos(),
                () -> "steps total " + stepsTotal + "ns must not exceed frame " + tree.durationNanos() + "ns");
    }

    /** The executed step with the given label. */
    private static CallNode.Step step(CallNode tree, String label) {
        return tree.steps().stream()
                .filter(step -> label.equals(step.label()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no executed step " + label + " in "
                        + tree.steps().stream().map(CallNode.Step::label).toList()));
    }

    /** The kinds of a step's children, in execution order. */
    private static List<FrameKind> kinds(CallNode.Step step) {
        return step.children().stream().map(CallNode::kind).toList();
    }
}
