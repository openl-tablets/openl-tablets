package org.openl.studio.projects.service.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.model.trace.DebugStatus;
import org.openl.studio.projects.model.trace.FrameKind;

/**
 * The business full run debugs with break-on-exception off (see {@link TraceDebugger#setBreakOnErrors}), so a
 * rule error ends the run and the executed tree — with the failed branch — is kept for offline browsing.
 *
 * <p>Fixture {@code errorProject.xlsx}: {@code DeterminePolicy} calls {@code DetermineManual}, which calls
 * {@code Validate}, whose {@code Result} cell raises {@code error("Some text")}. The two calling steps
 * ({@code $ManualRates}, {@code $Validation}) never return — they are interrupted mid-call by the error.
 *
 * <p>Fixture {@code directErrorProject.xlsx}: {@code Boom.Fail} is {@code error("boom")} with no nested
 * cell and no sub-call, so the failing step is present only as the frame's location until the completed
 * tree synthesizes it.
 */
class ErrorCallTreeTest {

    private static final String SRC = "test/rules/trace-debug/errorProject.xlsx";
    /** A single spreadsheet whose Fail step throws with no nested re-read and no sub-call. */
    private static final String DIRECT_SRC = "test/rules/trace-debug/directErrorProject.xlsx";

    @Test
    @DisplayName("A step interrupted by an error in the table it called keeps its own name, not a raw RnCm ref")
    void keepsRealStepNamesOnTheFailedBranch() {
        var tree = runToError(false);

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
    @DisplayName("The detailed business view marks every step on the failed path = ERROR, like the legacy trace")
    void marksTheFailedPathInTheDetailedView() {
        var tree = runToError(true);
        var labels = new ArrayList<String>();
        collectStepLabels(tree, labels);
        // Every step on the failed path — the callers and the cell that raised the error — reads "= ERROR",
        // so the tree itself is an error trail the user can follow without getting lost, like the legacy trace.
        assertTrue(labels.contains("$ManualRates = ERROR"), () -> "no marked $ManualRates in " + labels);
        assertTrue(labels.contains("$Validation = ERROR"), () -> "no marked $Validation in " + labels);
        assertTrue(labels.contains("$Result = ERROR"), () -> "no marked $Result in " + labels);
        // An interrupted caller is recorded via its sub-call; synthesizing the failing location must not
        // list that same step a second time (e.g. "$Coverages = ERROR" twice under one table).
        assertEquals(1, labels.stream().filter("$ManualRates = ERROR"::equals).count(),
                () -> "duplicate interrupted caller in " + labels);
        assertEquals(1, labels.stream().filter("$Validation = ERROR"::equals).count(),
                () -> "duplicate interrupted caller in " + labels);
        assertEquals(1, labels.stream().filter("$Result = ERROR"::equals).count(),
                () -> "duplicate failing step in " + labels);
    }

    @Test
    @DisplayName("Running through the error keeps the whole executed tree with the failed branch")
    void keepsTheExecutedTreeOnError() {
        var tree = runToError(false);
        assertEquals("DeterminePolicy", tree.name());
        // Base computed before the error and stays; the failing branch is retained under $ManualRates.
        assertNotNull(step(tree, "$Base"), "steps that finished before the error are kept");
        assertEquals(List.of("Validate"),
                childNames(step(step(tree, "$ManualRates").children().getFirst(), "$Validation")));
    }

    @Test
    @DisplayName("A step whose own formula throws (no sub-call) still appears under its table as = ERROR")
    void keepsTheFailingStepWhenTheFormulaThrowsDirectly() {
        // Fixture directErrorProject.xlsx: Boom.Fail is `= error("boom")` with no nested cell and no called
        // table. Without synthesizing that step from the frame's failing location, the business tree would
        // show only `Boom = ERROR` as a leaf — while Advanced still highlights Fail on the live stack.
        var tree = runMethodToError(DIRECT_SRC, "Boom", true, new Object[]{});
        var labels = new ArrayList<String>();
        collectStepLabels(tree, labels);
        assertTrue(labels.contains("$Fail = ERROR"), () -> "no marked $Fail in " + labels);
        assertTrue(step(tree, "$Fail = ERROR").children().isEmpty(), "the failing step is a leaf");
    }

    @Test
    @DisplayName("A non-spreadsheet frame that throws in its own body marks its failing step = ERROR")
    void marksTheFailingStepOfANonSpreadsheetFrame() {
        // A TBasic operation is not a spreadsheet cell, so it has no resolvable cell name; the failing step
        // must still fall back to the operation's own label and carry the "= ERROR" marker in the detailed
        // (business) view — otherwise it renders as a plain, unmarked line off the error trail.
        var descriptor = new SourceClassifier.FrameDescriptor(FrameKind.TBASIC, "uAlgo", "Algo");
        var frame = new DebugFrame(descriptor, new Object(), null, new Object[0], null, 0);
        frame.setLocation(CurrentLocation.operation("Step1"));
        frame.markError(new RuntimeException("boom"));

        var steps = frame.toCallNode(UnaryOperator.identity(), true).steps();

        assertEquals(1, steps.size(), () -> "one synthesized failing step, got " + steps);
        assertEquals("Step1 = ERROR", steps.getFirst().label(), "the operation's own label carries the marker");
        assertTrue(steps.getFirst().children().isEmpty(), "the failing operation is a leaf");
    }

    /** Run DeterminePolicy through the error the way the business view does, and return the kept tree. */
    private static CallNode runToError(boolean detailedTitles) {
        return runMethodToError(SRC, "DeterminePolicy", detailedTitles, new Object[]{1});
    }

    /** Run a named method through a rule error with break-on-exception off, and return the kept tree. */
    private static CallNode runMethodToError(String src, String methodName, boolean detailedTitles, Object[] args) {
        var compiled = new RulesEngineFactory<>(src).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> "module must compile: " + compiled.getAllMessages());
        var module = compiled.getOpenClass();
        var method = module.getMethods().stream()
                .filter(candidate -> methodName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setBreakOnErrors(false);
        debugger.start("error-tree", compiled.getClassLoader(), false, true, detailedTitles, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            var target = module.newInstance(env);
            method.invoke(target, args, env);
        });
        assertEquals(DebugStatus.ERROR, debugger.awaitInitialHalt(10_000),
                "the error ends the run instead of parking on it");
        var tree = debugger.completedTree();
        assertNotNull(tree, "the executed tree outlives the failed run");
        return tree;
    }

    /** The executed step with the given label (exact match, including a detailed {@code = ERROR} suffix). */
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

    /** Every step label in the tree, depth-first, for asserting the error marking down the whole path. */
    private static void collectStepLabels(CallNode node, List<String> out) {
        for (CallNode.Step step : node.steps()) {
            out.add(step.label());
            step.children().forEach(child -> collectStepLabels(child, out));
        }
    }

    /** No step is labelled as its own {@code RnCm} reference — every one resolved to a real name. */
    private static void assertNoRawRefLabels(CallNode node) {
        for (CallNode.Step step : node.steps()) {
            assertNotEquals(step.ref(), step.label(),
                    () -> "step " + step.ref() + " fell back to its raw reference as a label");
            step.children().forEach(ErrorCallTreeTest::assertNoRawRefLabels);
        }
    }
}
