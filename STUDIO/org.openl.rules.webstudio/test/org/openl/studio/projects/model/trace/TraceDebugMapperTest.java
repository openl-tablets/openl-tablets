package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.rules.webstudio.web.trace.debug.CallNode;
import org.openl.rules.webstudio.web.trace.debug.ConditionCheck;
import org.openl.rules.webstudio.web.trace.debug.DebugCommand;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.rules.webstudio.web.trace.debug.FrameKind;
import org.openl.rules.webstudio.web.trace.debug.SourceClassifier;
import org.openl.rules.webstudio.web.trace.debug.TraceDebugger;
import org.openl.rules.webstudio.web.trace.debug.WatchCapture;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Validates stack mapping and frame freezing against a real suspended debug session.
 */
class TraceDebugMapperTest {

    private static final String SRC = "test/rules/EPBDS-16160/generalProject.xlsx";

    private TraceDebugMapper mapper() {
        var objectMapper = new ObjectMapper();
        var schemaGenerator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
        return new TraceDebugMapper(objectMapper, schemaGenerator, new TraceParameterRegistry());
    }

    @Test
    void mapsStackAndFreezesFrameVariables() {
        CompiledOpenClass compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClass();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("mapper-test", compiled.getClassLoader(), true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));

        try {
            TraceDebugMapper mapper = mapper();
            List<DebugFrame> stack = debugger.stack();

            var stackView = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null);
            assertEquals(DebugStatus.SUSPENDED, stackView.status());
            assertFalse(stackView.frames().isEmpty(), "a suspended session has a stack");
            var top = stackView.frames().get(stackView.frames().size() - 1);
            assertEquals(FrameKind.SPREADSHEET, top.kind());
            assertEquals("MyRule", top.name());
            assertTrue(top.active(), "the deepest frame is the active one");

            var variables = mapper.freezeVariables(stack.get(stack.size() - 1), compiled.getClassLoader());
            assertNotNull(variables);
            assertTrue(variables.parameters().isEmpty(), "MyRule takes no parameters");
            assertNotNull(variables.context(), "the runtime context is frozen");
            assertNotNull(variables.context().value(), "a non-lazy context carries its serialized value");
            assertTrue(variables.errors().isEmpty());

            // Spreadsheet steps are enumerated with their real OpenL names ($...$Step), not raw cell refs.
            assertFalse(variables.steps().isEmpty(), "spreadsheet steps are enumerated");
            assertTrue(variables.steps().stream().allMatch(s -> s.label() != null && s.label().startsWith("$")),
                    "steps use the OpenL cell name, not the R0C0 reference");
        } finally {
            debugger.terminate(10_000);
        }
    }

    @Test
    void carriesPerFrameStepOutlineWithoutValues() {
        CompiledOpenClass compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClass();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("outline-test", compiled.getClassLoader(), true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));

        try {
            List<DebugFrame> stack = debugger.stack();
            var stackView = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null);
            var top = stackView.frames().get(stackView.frames().size() - 1);

            assertNotNull(top.steps(), "every frame carries its step outline");
            assertFalse(top.steps().isEmpty(), "a spreadsheet frame outlines its cells");
            // The outline is structure only: a status for each cell and no frozen value (kept cheap, no clone).
            assertTrue(top.steps().stream().allMatch(s -> s.status() != null),
                    "each step has a valid status");
            assertTrue(top.steps().stream().allMatch(s -> s.value() == null),
                    "the stack outline never carries values; they are fetched per frame on demand");
            assertTrue(top.steps().stream().allMatch(s -> s.label() != null && s.label().startsWith("$")),
                    "steps use the OpenL cell name");
        } finally {
            debugger.terminate(10_000);
        }
    }

    @Test
    void buildsAFriendlyErrorWithTableAndTechnicalDetail() {
        CompiledOpenClass compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        IOpenClass module = compiled.getOpenClass();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        TraceDebugger debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("error-mapper-test", compiled.getClassLoader(), true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));

        try {
            // Advance one step so the current frame carries a location to report.
            assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, 10_000));
            List<DebugFrame> stack = debugger.stack();

            var errorView = TraceDebugMapper.toStackView(DebugStatus.ERROR, stack, new IllegalStateException("kaboom"));
            DebugError error = errorView.error();
            assertNotNull(error, "an errored session carries a structured error");
            assertEquals("MyRule", error.table(), "the failing table is named");
            assertEquals("IllegalStateException", error.type(), "the technical type is exposed for drill-down");
            assertNotNull(error.summary(), "a human-readable summary is always present");
            assertNotNull(error.detail(), "the stack trace is available as technical detail");
            assertTrue(error.detail().contains("IllegalStateException"), "the detail carries the stack trace");

            // A healthy stack carries no error.
            assertNull(TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null).error());
        } finally {
            debugger.terminate(10_000);
        }
    }

    @Test
    void buildsADecisionExplanationFromConditionChecks() {
        IDecisionTable dt = mock(IDecisionTable.class);
        when(dt.getRuleName(0)).thenReturn("Standard");
        when(dt.getRuleName(1)).thenReturn("Senior");
        IBaseCondition age = mock(IBaseCondition.class);
        when(age.getName()).thenReturn("Age");
        IBaseCondition state = mock(IBaseCondition.class);
        when(state.getName()).thenReturn("State");

        var checks = List.of(
                new ConditionCheck(age, new int[]{0, 1}, true),   // Age matched for both rules
                new ConditionCheck(state, new int[]{0}, true),     // State matched for Standard
                new ConditionCheck(state, new int[]{1}, false));   // State failed for Senior

        var decision = TraceDebugMapper.buildDecision(dt, checks, new int[]{0});  // rule 0 fired
        assertNotNull(decision);
        assertEquals(List.of("Standard"), decision.firedRules());
        // One row per checked condition cell, mirroring the green/red highlight: 2 + 1 + 1.
        assertEquals(4, decision.conditions().size());
        assertTrue(decision.conditions().stream().anyMatch(
                c -> c.condition().equals("State") && c.rule().equals("Senior") && !c.matched()));
        assertTrue(decision.conditions().stream().anyMatch(
                c -> c.condition().equals("Age") && c.rule().equals("Standard") && c.matched()));

        // Suspended at entry: nothing evaluated and no rule fired → no explanation.
        assertNull(TraceDebugMapper.buildDecision(dt, List.of(), new int[0]));
    }

    @Test
    void outlinesDecisionTableRulesMarkingTheFiredOneCurrent() {
        IDecisionTable dt = mock(IDecisionTable.class);
        when(dt.getNumberOfRules()).thenReturn(3);
        when(dt.getRuleName(0)).thenReturn("R1");
        when(dt.getRuleName(1)).thenReturn("R2");
        when(dt.getRuleName(2)).thenReturn("R3");

        var steps = TraceDebugMapper.ruleOutline(dt, new int[]{0});  // R1 fired and is mid-action

        assertEquals(List.of("R1", "R2", "R3"), steps.stream().map(StepValueView::ref).toList());
        // The fired rule is current so a sub-table called from its action nests under it, not the last rule.
        assertEquals(StepStatus.CURRENT, steps.get(0).status(), "the firing rule is the current one");
        assertEquals(StepStatus.PENDING, steps.get(1).status(), "rules that did not fire are pending");
        assertEquals(StepStatus.PENDING, steps.get(2).status(), "rules that did not fire are pending");
        assertTrue(steps.stream().allMatch(s -> s.value() == null), "the outline carries no values");

        // Nothing fired yet (suspended at entry): every rule is pending and run-to-able.
        assertTrue(TraceDebugMapper.ruleOutline(dt, new int[0]).stream().allMatch(s -> s.status() == StepStatus.PENDING));
    }

    @Test
    void listsEveryDistinctRuleNameSoAnyRuleCanBeArmed() {
        IDecisionTable dt = mock(IDecisionTable.class);
        when(dt.getNumberOfRules()).thenReturn(4);
        when(dt.getRuleName(0)).thenReturn("R1");
        when(dt.getRuleName(1)).thenReturn("R2");
        when(dt.getRuleName(2)).thenReturn("R3");
        when(dt.getRuleName(3)).thenReturn("R2");  // a duplicate name collapses to one

        assertEquals(List.of("R1", "R2", "R3"), TraceDebugMapper.ruleNames(dt));
    }

    @Test
    void foldsExecutedTreeIntoSelfTimeHotspots() {
        // A(100) calls B(30) and C(50); C calls B(20) again; A also references B (no time).
        CallNode b1 = leaf("uB", "B", 30);
        CallNode b2 = leaf("uB", "B", 20);
        CallNode c = node("uC", "C", 50, b2);
        CallNode ref = new CallNode("uA", "$Ref", FrameKind.STEP_REF, 0, List.of(), null, "R9C9");
        CallNode root = new CallNode("uA", "A", FrameKind.SPREADSHEET, ms(100),
                List.of(new CallNode.Step("R0C0", "$s", ms(100), List.of(b1, c, ref))), null, null);

        var summary = TraceDebugMapper.buildProfileSummary(root, 10);

        assertEquals(3, summary.distinctTables(), "A, B, C — the reference is not a table");
        assertEquals(4, summary.nodeCount(), "A + B + C + B again; the reference does not count");
        assertEquals(100.0, summary.totalMillis(), "wall-clock is the root's own duration");
        assertFalse(summary.truncated(), "all three tables fit within top=10");

        // Sorted by own time: B has the most (30 + 20), then C (50 − 20), then A (100 − 30 − 50).
        var b = summary.hotspots().get(0);
        assertEquals("uB", b.uri());
        assertEquals(2, b.count(), "both B invocations fold into one hotspot");
        assertEquals(50.0, b.selfMillis());
        assertEquals(50.0, b.totalMillis(), "B calls nothing, so total equals self");
        assertEquals(List.of("uB", "uC", "uA"), summary.hotspots().stream().map(ProfileHotspotView::uri).toList());
        assertEquals(30.0, summary.hotspots().get(1).selfMillis(), "C's own time excludes the B it called");
        assertEquals(20.0, summary.hotspots().get(2).selfMillis(), "A's own time excludes B and C");
    }

    @Test
    void capsHotspotsToTopAndFlagsTruncation() {
        CallNode root = new CallNode("uA", "A", FrameKind.SPREADSHEET, ms(60),
                List.of(new CallNode.Step("R0C0", "$s", ms(60),
                        List.of(leaf("uB", "B", 30), leaf("uC", "C", 20)))), null, null);

        var summary = TraceDebugMapper.buildProfileSummary(root, 2);

        assertEquals(3, summary.distinctTables());
        assertEquals(2, summary.hotspots().size(), "only the two slowest are returned");
        assertTrue(summary.truncated(), "a third table ran but did not make the cut");
        assertEquals(List.of("uB", "uC"), summary.hotspots().stream().map(ProfileHotspotView::uri).toList());
    }

    @Test
    void flagsTruncationWhenTheExecutedTreeHitTheNodeCap() {
        CallNode root = new CallNode("uA", "A", FrameKind.SPREADSHEET, ms(60),
                List.of(new CallNode.Step("R0C0", "$s", ms(60), List.of(leaf("uB", "B", 30)))), null, null);

        // Both tables fit within top=10, so only the tree-node cap can mark the profile incomplete.
        var summary = TraceDebugMapper.buildProfileSummary(root, 10, true);

        assertEquals(2, summary.hotspots().size(), "both tables are returned, so hotspots are complete");
        assertTrue(summary.truncated(), "the node cap alone marks the profile incomplete");
    }

    @Test
    void compactViewKeepsStepsOnlyOnTheActiveFrame() {
        List<DebugFrame> stack = List.of(
                debugFrame(FrameKind.METHOD, "uRoot", "Root", 1),
                debugFrame(FrameKind.SPREADSHEET, "uChild", "Child", 2));

        var full = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null, null, StackRenderOptions.FULL);
        assertNotNull(full.frames().get(0).steps(), "full view keeps every frame's steps");
        assertNotNull(full.frames().get(1).steps());

        var compact = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null, null,
                new StackRenderOptions(true, TraceDebugMapper.DEFAULT_PROFILE_TOP, true));
        assertNull(compact.frames().get(0).steps(), "compact drops the non-active frame's steps");
        assertTrue(compact.frames().get(1).active(), "the top frame is the active one");
        assertNotNull(compact.frames().get(1).steps(), "the active frame keeps its steps");
    }

    @Test
    void groupsWatchCapturesIntoPerCellSeriesInExecutionOrder() {
        List<WatchCapture> captures = List.of(
                new WatchCapture("$Factor", "Cov", "uCov", 0, List.of("Root", "Cov"), "uCov#R2C0", 1.0),
                new WatchCapture("$Factor", "Cov", "uCov", 1, List.of("Root", "Cov"), "uCov#R2C0", 83.372),
                new WatchCapture("$Other", "Cov", "uCov", 0, List.of("Root", "Cov"), "uCov#R3C0", 2.0));

        var view = mapper().toWatchView(captures, false, null);

        assertEquals(2, view.series().size(), "two distinct cells → two series");
        var factor = view.series().get(0);
        assertEquals("$Factor", factor.name());
        assertEquals("uCov", factor.tableUri());
        assertEquals(List.of(1.0, 83.372),
                factor.points().stream().map(p -> p.value().value().asDouble()).toList(),
                "the factor's values across both executions, in order");
        assertEquals("Cov #2", factor.points().get(1).label(), "instance 1 reads as the 2nd execution");
        assertEquals("uCov#R2C0", factor.points().get(0).ref());
        assertEquals(2, factor.total(), "both executions counted in the total");
        assertFalse(view.truncated());
    }

    @Test
    void capsWatchSeriesPointsButReportsTheFullTotal() {
        List<WatchCapture> captures = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            captures.add(new WatchCapture("$Loop", "T", "uT", i, List.of("T"), "uT#R0C0", i));
        }

        var series = mapper().toWatchView(captures, false, null).series().get(0);

        assertEquals(100, series.points().size(), "a factor looped 150 times returns only the first 100 points");
        assertEquals(150, series.total(), "the full execution count is still reported");
        assertEquals(0.0, series.points().get(0).value().value().asDouble(), "points keep execution order from the start");
    }

    @Test
    void splitsTheSameCellNameInDifferentTablesIntoSeparateSeries() {
        List<WatchCapture> captures = List.of(
                new WatchCapture("$X", "A", "uA", 0, List.of("A"), "uA#R0C0", 1),
                new WatchCapture("$X", "B", "uB", 0, List.of("B"), "uB#R0C0", 2));

        var view = mapper().toWatchView(captures, true, null);

        assertEquals(2, view.series().size(), "the same name in two tables stays two series");
        assertTrue(view.truncated(), "the cap flag is carried through");
    }

    private static DebugFrame debugFrame(FrameKind kind, String uri, String name, int depth) {
        return new DebugFrame(new SourceClassifier.FrameDescriptor(kind, uri, name),
                new Object(), null, new Object[0], null, depth);
    }

    private static CallNode leaf(String uri, String name, long millis) {
        return new CallNode(uri, name, FrameKind.SPREADSHEET, ms(millis), List.of(), null, null);
    }

    private static CallNode node(String uri, String name, long millis, CallNode child) {
        return new CallNode(uri, name, FrameKind.SPREADSHEET, ms(millis),
                List.of(new CallNode.Step("R0C0", "$s", ms(millis), List.of(child))), null, null);
    }

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }
}
