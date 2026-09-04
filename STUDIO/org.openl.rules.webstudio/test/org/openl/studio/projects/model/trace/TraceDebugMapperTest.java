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

import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.service.trace.CallNode;
import org.openl.studio.projects.service.trace.ConditionCheck;
import org.openl.studio.projects.service.trace.DebugCommand;
import org.openl.studio.projects.service.trace.DebugFrame;
import org.openl.studio.projects.service.trace.DebugListener;
import org.openl.studio.projects.service.trace.SourceClassifier;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.studio.projects.service.trace.WatchCapture;
import org.openl.types.IOpenClass;

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
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("mapper-test", compiled.getClassLoader(), true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));

        try {
            var mapper = mapper();
            List<DebugFrame> stack = debugger.stack();

            var stackView = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null);
            assertEquals(DebugStatus.SUSPENDED, stackView.status());
            assertFalse(stackView.frames().isEmpty(), "a suspended session has a stack");
            var top = stackView.frames().get(stackView.frames().size() - 1);
            assertEquals(FrameKind.SPREADSHEET, top.kind());
            assertEquals("MyRule", top.name());
            assertTrue(top.active(), "the deepest frame is the active one");

            var variables = mapper.freezeVariables(stack.getLast(), compiled.getClassLoader(), true);
            assertNotNull(variables);
            assertTrue(variables.parameters().isEmpty(), "MyRule takes no parameters");
            assertNotNull(variables.context(), "the runtime context is frozen");
            assertNotNull(variables.context().value(), "a non-lazy context carries its serialized value");
            assertTrue(variables.errors().isEmpty());

            // Spreadsheet steps are enumerated with their real OpenL names ($...$Step), not raw cell refs.
            assertFalse(variables.steps().isEmpty(), "spreadsheet steps are enumerated");
            assertTrue(variables.steps().stream().allMatch(s -> s.label() != null && s.label().startsWith("$")),
                    "steps use the OpenL cell name, not the R0C0 reference");
            assertTrue(variables.steps().stream().allMatch(s -> s.cell() != null && s.cell().matches("[A-Z]+\\d+")),
                    "each spreadsheet step carries the A1 address of its source cell");
        } finally {
            debugger.terminate(10_000);
        }
    }

    @Test
    void carriesPerFrameStepOutlineWithoutValues() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("outline-test", compiled.getClassLoader(), true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
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
            assertTrue(top.steps().stream().allMatch(s -> s.cell() != null && s.cell().matches("[A-Z]+\\d+")),
                    "the outline still points at each step's source cell for client-side highlighting");
        } finally {
            debugger.terminate(10_000);
        }
    }

    @Test
    void buildsAFriendlyErrorWithTableAndTechnicalDetail() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("error-mapper-test", compiled.getClassLoader(), true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            myRule.invoke(module.newInstance(env), new Object[0], env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(10_000));

        try {
            // Advance one step so the current frame carries a location to report.
            assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_INTO, 10_000));
            List<DebugFrame> stack = debugger.stack();

            var errorView = TraceDebugMapper.toStackView(DebugStatus.ERROR, stack, new IllegalStateException("kaboom"));
            var error = errorView.error();
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
        assertEquals(StepStatus.CURRENT, steps.getFirst().status(), "the firing rule is the current one");
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
    void shapesStatsIntoSortedHotspots() {
        // Per-table stats already aggregated by the hook: B ran twice (self 50), C once (self 30), A once (20).
        var stats = List.of(
                new TableProfile("uA", "A", FrameKind.SPREADSHEET, 1, ms(20), ms(100)),
                new TableProfile("uB", "B", FrameKind.SPREADSHEET, 2, ms(50), ms(50)),
                new TableProfile("uC", "C", FrameKind.SPREADSHEET, 1, ms(30), ms(50)));

        var summary = TraceDebugMapper.buildProfileSummary(stats, 10, ms(100), false);

        assertEquals(3, summary.distinctTables(), "A, B, C");
        assertEquals(4, summary.nodeCount(), "every invocation counts: A + B + B + C");
        assertEquals(100.0, summary.totalMillis(), "wall-clock is the passed root duration");
        assertFalse(summary.truncated(), "the tree was not truncated");

        // Sorted by own time: B (50), C (30), A (20).
        assertEquals(List.of("uB", "uC", "uA"), summary.hotspots().stream().map(ProfileHotspotView::uri).toList());
        var b = summary.hotspots().getFirst();
        assertEquals(2, b.count(), "both B invocations are one hotspot row");
        assertEquals(50.0, b.selfMillis());
        assertEquals(50.0, b.totalMillis());
        assertEquals(30.0, summary.hotspots().get(1).selfMillis());
        assertEquals(20.0, summary.hotspots().get(2).selfMillis());
    }

    @Test
    void capsHotspotsToTopWithoutFlaggingTruncation() {
        var stats = List.of(
                new TableProfile("uA", "A", FrameKind.SPREADSHEET, 1, ms(10), ms(60)),
                new TableProfile("uB", "B", FrameKind.SPREADSHEET, 1, ms(30), ms(30)),
                new TableProfile("uC", "C", FrameKind.SPREADSHEET, 1, ms(20), ms(20)));

        var summary = TraceDebugMapper.buildProfileSummary(stats, 2, ms(60), false);

        assertEquals(3, summary.distinctTables());
        assertEquals(2, summary.hotspots().size(), "only the two slowest are returned");
        assertFalse(summary.truncated(), "top-N is a display limit, not truncation — every table was counted");
        assertEquals(List.of("uB", "uC"), summary.hotspots().stream().map(ProfileHotspotView::uri).toList());
    }

    @Test
    void flagsTruncationWhenTheExecutedTreeHitTheNodeCap() {
        var stats = List.of(
                new TableProfile("uA", "A", FrameKind.SPREADSHEET, 1, ms(30), ms(60)),
                new TableProfile("uB", "B", FrameKind.SPREADSHEET, 1, ms(30), ms(30)));

        // The hotspots are complete; only the executed tree hit its node cap.
        var summary = TraceDebugMapper.buildProfileSummary(stats, 10, ms(60), true);

        assertEquals(2, summary.hotspots().size(), "the hotspots themselves are complete");
        assertTrue(summary.truncated(), "the tree hit its node cap, so it is flagged incomplete");
    }

    @Test
    void pagesALoopsChildrenAtTheRequestedLimit() {
        // A single step that called table B 150 times in a loop.
        var kids = new ArrayList<CallNode>();
        for (var i = 0; i < 150; i++) {
            kids.add(leaf("uB", "B", 1));
        }
        var root = new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, ms(200),
                List.of(new CallNode.Step("R0C0", "$s", ms(200), kids)), null, null);

        var page = TraceDebugMapper.toChildrenView(root, "uA", 0, "R0C0", 0, 100);

        assertEquals(100, page.children().size(), "a step looped 150 times returns only the first 100 per page");
        assertEquals(150, page.total(), "the full count is reported so the client can page through the rest");
    }

    @Test
    void serializesTheCompletedTreeOneLevelDeepForLazyLoading() {
        var tree = TraceDebugMapper.toStackView(DebugStatus.COMPLETED, List.of(), null, lazyTreeFixture(), List.of(),
                StackRenderOptions.FULL, false).tree();

        assertNotNull(tree);
        assertEquals("A", tree.name());
        var step = tree.steps().getFirst();
        assertNull(step.children(), "children are lazy — not serialized with the root");
        assertEquals(2, step.childrenTotal(), "the child count is reported so the client shows the step as expandable");
    }

    @Test
    void fullTreeSerializesTheWholeTreeDeepInOneResponse() {
        // The business view asks for the full tree: instead of the shallow lazy root, every step's sub-calls
        // are inline, recursively, so the client browses the whole tree offline without paging.
        var tree = TraceDebugMapper.toStackView(DebugStatus.COMPLETED, List.of(), null, lazyTreeFixture(), List.of(),
                new StackRenderOptions(true, TraceDebugMapper.DEFAULT_PROFILE_TOP, false, true), false).tree();

        assertNotNull(tree);
        var step = tree.steps().get(0);
        assertNotNull(step.children(), "the full tree carries the step's sub-calls inline, not lazily");
        assertEquals(List.of("B", "C"), step.children().stream().map(CallNodeView::name).toList());
        // Depth too: B's own loop iterations are present without a second request.
        var bLoop = step.children().get(0).steps().get(0);
        assertNotNull(bLoop.children(), "grandchildren are inline as well — the whole subtree is one payload");
        assertEquals(3, bLoop.children().size());
    }

    @Test
    void fullTreeCapsAStepAtTheChildLimitAndReportsTheOmittedCount() {
        // A step looped past MAX_TREE_CHILDREN keeps only the first 100 inline; the full count is reported so
        // the client marks how many executions are omitted.
        List<CallNode> kids = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            kids.add(leaf("uB", "B", 1));
        }
        CallNode root = new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, ms(200),
                List.of(new CallNode.Step("R0C0", "$s", ms(200), kids)), null, null);

        var step = TraceDebugMapper.toCappedTree(root).steps().get(0);

        assertEquals(100, step.children().size(), "only the first 100 iterations are serialized inline");
        assertEquals(150, step.childrenTotal(), "the full count marks the branch as truncated");
    }

    @Test
    void fullTreeCutsBranchesBeyondTheNodeBudgetAndMarksThemTruncated() {
        // With a tiny budget the deep serialization stops once the budget is spent: the first sub-call is
        // included, the rest are cut and their step reports the full count so the client shows the truncation.
        CallNode root = new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, ms(20),
                List.of(new CallNode.Step("R0C0", "$s", ms(20),
                        List.of(leaf("uB", "B", 5), leaf("uC", "C", 5), leaf("uD", "D", 5)))), null, null);

        var step = TraceDebugMapper.toCappedTree(root, 1).steps().get(0);

        assertEquals(1, step.children().size(), "the budget admitted only the first sub-call");
        assertEquals("B", step.children().get(0).name());
        assertEquals(3, step.childrenTotal(), "the omitted sub-calls are reported as truncated");
    }

    @Test
    void fetchesAStepsChildrenLazilyOneLevelDeep() {
        CallNode root = lazyTreeFixture();

        // Expand the root's step: returns B and C, each shallow (B's own loop step stays unexpanded).
        var top = TraceDebugMapper.toChildrenView(root, "uA", 0, "R0C0", 0, 100);
        assertEquals(2, top.total());
        assertEquals(List.of("B", "C"), top.children().stream().map(CallNodeView::name).toList());
        assertNull(top.children().getFirst().steps().getFirst().children(), "B's grandchildren stay lazy");
        assertEquals(3, top.children().getFirst().steps().getFirst().childrenTotal(), "B's loop count is reported");

        // Expand B's loop step by its (uri, instance): returns its three iterations.
        var loop = TraceDebugMapper.toChildrenView(root, "uB", 0, "R1C1", 0, 100);
        assertEquals(3, loop.total());
        assertEquals(List.of("D", "E", "F"), loop.children().stream().map(CallNodeView::name).toList());
    }

    @Test
    void pagesThroughALoopsChildren() {
        var page = TraceDebugMapper.toChildrenView(lazyTreeFixture(), "uB", 0, "R1C1", 1, 1);

        assertEquals(3, page.total(), "the total stays the full count so the client can page through the rest");
        assertEquals(List.of("E"), page.children().stream().map(CallNodeView::name).toList());
    }

    @Test
    void returnsAnEmptyPageWhenTheFrameOrStepIsNoLongerRetained() {
        assertEquals(0, TraceDebugMapper.toChildrenView(lazyTreeFixture(), "uMissing", 9, "R0C0", 0, 100).total());
        assertEquals(0, TraceDebugMapper.toChildrenView(null, "uA", 0, "R0C0", 0, 100).total());
    }

    @Test
    void marksExpandableStepsWithTheirChildCountAndChildlessStepsWithout() {
        var root = new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, ms(10),
                List.of(new CallNode.Step("R0C0", "$called", ms(10), List.of(leaf("uB", "B", 5))),
                        new CallNode.Step("R1C0", "$plain", ms(1), List.of())), null, null);

        var steps = TraceDebugMapper.toStackView(DebugStatus.COMPLETED, List.of(), null, root, List.of(),
                StackRenderOptions.FULL, false).tree().steps();

        // A step that made a call: children are lazy, but its count is reported so it shows as expandable.
        assertNull(steps.getFirst().children(), "children are lazy — fetched on expand");
        assertEquals(1, steps.getFirst().childrenTotal());
        // A step that called nothing: no count, so no expand affordance.
        assertNull(steps.get(1).children());
        assertNull(steps.get(1).childrenTotal(), "a step that called nothing carries no child count");
    }

    @Test
    void reportsDroppedSubCallsSoTheTruncationGapIsVisible() {
        var dropped = new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, ms(10), List.of(), null, null, 42, 0);
        var kept = new CallNode("uB", "B", 0, FrameKind.SPREADSHEET, ms(10), List.of(), null, null);

        assertEquals(42L, treeOf(dropped).notRetained(), "a node reports how many of its sub-calls were dropped");
        assertNull(treeOf(kept).notRetained(), "a node that kept every sub-call carries no dropped count");
    }

    private static CallNodeView treeOf(CallNode root) {
        return TraceDebugMapper.toStackView(DebugStatus.COMPLETED, List.of(), null, root, List.of(),
                StackRenderOptions.FULL, false).tree();
    }

    @Test
    void mapsTheCallNodeExecutionIndexSoALoopIterationCanBeReplayed() {
        var root = new CallNode("uA", "A", 7, FrameKind.SPREADSHEET, ms(10), List.of(), null, null);

        var view = TraceDebugMapper.toStackView(DebugStatus.COMPLETED, List.of(), null, root, List.of(),
                StackRenderOptions.FULL, false);

        assertNotNull(view.tree());
        assertEquals(7, view.tree().instance(), "the node carries its execution index so a uri@N replay hits it");
    }

    @Test
    void compactViewKeepsStepsOnlyOnTheActiveFrame() {
        var stack = List.of(
                debugFrame(FrameKind.METHOD, "uRoot", "Root", 1),
                debugFrame(FrameKind.SPREADSHEET, "uChild", "Child", 2));

        var full = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null, null, List.of(),
                StackRenderOptions.FULL, false);
        assertNotNull(full.frames().getFirst().steps(), "full view keeps every frame's steps");
        assertNotNull(full.frames().get(1).steps());

        var compact = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null, null, List.of(),
                new StackRenderOptions(true, TraceDebugMapper.DEFAULT_PROFILE_TOP, true, false), false);
        assertNull(compact.frames().getFirst().steps(), "compact drops the non-active frame's steps");
        assertTrue(compact.frames().get(1).active(), "the top frame is the active one");
        assertNotNull(compact.frames().get(1).steps(), "the active frame keeps its steps");
    }

    @Test
    void groupsWatchCapturesIntoPerCellSeriesInExecutionOrder() {
        var captures = List.of(
                new WatchCapture("$Factor", "Cov", "uCov", 0, List.of("Root", "Cov"), "uCov#R2C0", 1.0),
                new WatchCapture("$Factor", "Cov", "uCov", 1, List.of("Root", "Cov"), "uCov#R2C0", 83.372),
                new WatchCapture("$Other", "Cov", "uCov", 0, List.of("Root", "Cov"), "uCov#R3C0", 2.0));

        var view = mapper().toWatchView(captures, false, null, true);

        assertEquals(2, view.series().size(), "two distinct cells → two series");
        var factor = view.series().getFirst();
        assertEquals("$Factor", factor.name());
        assertEquals("uCov", factor.tableUri());
        assertEquals(List.of(1.0, 83.372),
                factor.points().stream().map(p -> p.value().value().asDouble()).toList(),
                "the factor's values across both executions, in order");
        assertEquals("Cov #2", factor.points().get(1).label(), "instance 1 reads as the 2nd execution");
        assertEquals("uCov#R2C0", factor.points().getFirst().ref());
        assertEquals(2, factor.total(), "both executions counted in the total");
        assertFalse(view.truncated());
    }

    @Test
    void capsWatchSeriesPointsButReportsTheFullTotal() {
        var captures = new ArrayList<WatchCapture>();
        for (var i = 0; i < 150; i++) {
            captures.add(new WatchCapture("$Loop", "T", "uT", i, List.of("T"), "uT#R0C0", i));
        }

        var series = mapper().toWatchView(captures, false, null, true).series().getFirst();

        assertEquals(100, series.points().size(), "a factor looped 150 times returns only the first 100 points");
        assertEquals(150, series.total(), "the full execution count is still reported");
        assertEquals(0.0, series.points().getFirst().value().value().asDouble(), "points keep execution order from the start");
    }

    @Test
    void splitsTheSameCellNameInDifferentTablesIntoSeparateSeries() {
        var captures = List.of(
                new WatchCapture("$X", "A", "uA", 0, List.of("A"), "uA#R0C0", 1),
                new WatchCapture("$X", "B", "uB", 0, List.of("B"), "uB#R0C0", 2));

        var view = mapper().toWatchView(captures, true, null, true);

        assertEquals(2, view.series().size(), "the same name in two tables stays two series");
        assertTrue(view.truncated(), "the cap flag is carried through");
    }

    private static DebugFrame debugFrame(FrameKind kind, String uri, String name, int depth) {
        return new DebugFrame(new SourceClassifier.FrameDescriptor(kind, uri, name),
                new Object(), null, new Object[0], null, depth);
    }

    private static CallNode leaf(String uri, String name, long millis) {
        return new CallNode(uri, name, 0, FrameKind.SPREADSHEET, ms(millis), List.of(), null, null);
    }

    /** Root A → step calls B and C; B → loop step calls D, E, F. Three levels, for the lazy-tree tests. */
    private static CallNode lazyTreeFixture() {
        var b = new CallNode("uB", "B", 0, FrameKind.SPREADSHEET, ms(10),
                List.of(new CallNode.Step("R1C1", "$loop", ms(10),
                        List.of(leaf("uD", "D", 1), leaf("uE", "E", 1), leaf("uF", "F", 1)))), null, null);
        return new CallNode("uA", "A", 0, FrameKind.SPREADSHEET, ms(20),
                List.of(new CallNode.Step("R0C0", "$s", ms(20), List.of(b, leaf("uC", "C", 5)))), null, null);
    }

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }
}
