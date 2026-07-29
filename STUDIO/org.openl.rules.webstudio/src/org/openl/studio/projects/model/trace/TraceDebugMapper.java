package org.openl.studio.projects.model.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.binding.ILocalVar;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.CustomSpreadsheetResultField;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.element.SpreadsheetRangeField;
import org.openl.rules.cloner.Cloner;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.types.DatatypeOpenField;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.table.xls.XlsUtil;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.config.SafeSchemaGenerator;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.service.trace.CallNode;
import org.openl.studio.projects.service.trace.ConditionCheck;
import org.openl.studio.projects.service.trace.CurrentLocation;
import org.openl.studio.projects.service.trace.DebugFrame;
import org.openl.studio.projects.service.trace.SpreadsheetCellNames;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.studio.projects.service.trace.WatchCapture;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;

/**
 * Maps the debugger's live stack to view models and freezes a frame's variables on demand.
 *
 * <p>Freezing deep-clones a frame's parameters, context, and result while execution is suspended, so
 * the snapshot stays stable even after execution resumes or the frame returns. Large values are
 * registered in the {@link TraceParameterRegistry} and fetched lazily.
 */
@Slf4j
@RequiredArgsConstructor
public class TraceDebugMapper {

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final TraceParameterRegistry parameterRegistry;

    /** Upper bound on the technical stack-trace detail, so a deep failure cannot bloat the response. */
    private static final int MAX_DETAIL = 8_000;

    /** Upper bound on points returned per watch series, so a factor looped thousands of times stays renderable. */
    private static final int MAX_POINTS_PER_SERIES = 100;

    /** Upper bound on executed children returned per step, so a table looped thousands of times stays renderable. */
    private static final int MAX_TREE_CHILDREN = 100;

    /**
     * Node budget for the one-shot full tree the business view downloads. The whole executed tree is
     * serialized deep up to this many nodes in a single response; a branch beyond it is cut and marked
     * truncated. This bounds the response so one request replaces the thousands of lazy page fetches.
     */
    private static final int MAX_FULL_TREE_NODES = 50_000;

    /** Default number of hotspots in the profile overview when the caller does not ask for a specific size. */
    public static final int DEFAULT_PROFILE_TOP = 20;

    /** Map the live stack (root to current frame) to a stack view with default full rendering. */
    public static DebugStackView toStackView(DebugStatus status, List<DebugFrame> frames, @Nullable Throwable error) {
        return toStackView(status, frames, error, null, List.of(), StackRenderOptions.FULL, false);
    }

    /**
     * Map the live stack to a stack view, shaped by {@code options}. Once the trace has finished in
     * profiling mode the completed tree is available; {@code includeTree} embeds it in full, and a bounded
     * profile overview (the slowest tables) is always attached so a large run can be understood without it.
     * In {@code compact} mode only the active frame carries its sub-steps, so a step no longer re-sends
     * every frame's steps. {@code treeTruncated} marks the profile incomplete when the tree hit the node cap.
     */
    public static DebugStackView toStackView(DebugStatus status, List<DebugFrame> frames, @Nullable Throwable error,
                                             @Nullable CallNode completedTree, List<TableProfile> profileStats,
                                             StackRenderOptions options, boolean treeTruncated) {
        var views = new ArrayList<DebugFrameView>(frames.size());
        for (var i = 0; i < frames.size(); i++) {
            var frame = frames.get(i);
            var active = i == frames.size() - 1;
            views.add(DebugFrameView.builder()
                    .index(i)
                    .depth(frame.getDepth())
                    .instance(frame.getInvocationIndex())
                    .uri(frame.getUri())
                    .tableId(TableUtils.makeTableId(frame.getUri()))
                    .name(frame.getName())
                    .kind(frame.getKind())
                    .location(toLocationView(frame.getLocation()))
                    .active(active)
                    .completed(frame.isCompleted())
                    .error(frame.getError() != null)
                    .steps(options.compact() && !active ? null : outlineSteps(frame, completedTree == null))
                    .durationMillis(completedMillis(frame))
                    .selfMillis(completedSelfMillis(frame))
                    .dispatch(frame.getDispatch())
                    .build());
        }
        return DebugStackView.builder()
                .status(status)
                .frames(views)
                .error(buildStackError(frames, error))
                .tree(completedTree == null || !options.includeTree() ? null
                        : options.fullTree() ? toCappedTree(completedTree)
                                : toShallowCallNodeView(completedTree))
                .profile(completedTree == null ? null
                        : buildProfileSummary(profileStats, options.profileTop(), completedTree.durationNanos(),
                                treeTruncated))
                .build();
    }

    /**
     * Group watched-cell captures into series: one per cell (scoped to its table), points in execution
     * order. Captures already arrive in execution order, so the points need no re-sorting. Each value is
     * deep-cloned and serialized to the rich parameter view (like frame variables), so dates, arrays, and
     * spreadsheet results render properly and large values load lazily.
     */
    public WatchView toWatchView(List<WatchCapture> captures, boolean truncated, @Nullable ClassLoader classLoader, boolean includeSchema) {
        var previous = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            // Group raw captures by series first, so a factor computed thousands of times in a loop only
            // serializes its first points: the client cannot render more, and the full response would be huge.
            // total still reports how many executions there were, so the outlier count stays visible.
            var byKey = new LinkedHashMap<String, List<WatchCapture>>();
            for (WatchCapture capture : captures) {
                byKey.computeIfAbsent(capture.name() + ' ' + capture.tableUri(), k -> new ArrayList<>()).add(capture);
            }
            var clones = new IdentityHashMap<Object, Object>();
            var series = new ArrayList<WatchSeriesView>(byKey.size());
            byKey.forEach((key, group) -> {
                var first = group.getFirst();
                List<WatchPointView> points = group.stream()
                        .limit(MAX_POINTS_PER_SERIES)
                        .map(capture -> toWatchPoint(capture, clones, includeSchema))
                        .toList();
                series.add(WatchSeriesView.builder()
                        .name(first.name())
                        .table(first.table())
                        .tableUri(first.tableUri())
                        .points(points)
                        .total(group.size())
                        .build());
            });
            return WatchView.builder().series(series).truncated(truncated).build();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private WatchPointView toWatchPoint(WatchCapture capture, Map<Object, Object> clones, boolean includeSchema) {
        var param = new ParameterWithValueDeclaration(capture.name(), safeClone(capture.value(), clones, true));
        return WatchPointView.builder()
                .instance(capture.instance())
                .label(capture.table() + " #" + (capture.instance() + 1))
                .path(capture.path())
                .ref(capture.ref())
                .value(buildParameterValue(param, true, includeSchema))
                .build();
    }

    /**
     * Shape the per-table stats gathered on the fly into a bounded hotspots overview: keep only the slowest
     * {@code top} tables by own time. Constant-sized regardless of run size.
     *
     * <p>The stats count every invocation the run made, so the overview stays accurate even when the executed
     * tree was truncated for size — {@code treeTruncated} then flags only that the tree is incomplete, not the
     * hotspots.
     */
    static ProfileSummaryView buildProfileSummary(List<TableProfile> stats, int top, long rootNanos,
                                                  boolean treeTruncated) {
        List<ProfileHotspotView> hotspots = stats.stream()
                .sorted(Comparator.comparingLong(TableProfile::selfNanos).reversed())
                .limit(Math.max(1, top))
                .map(TraceDebugMapper::toHotspotView)
                .toList();
        var invocations = stats.stream().mapToInt(TableProfile::count).sum();
        return ProfileSummaryView.builder()
                .hotspots(hotspots)
                .distinctTables(stats.size())
                .nodeCount(invocations)
                .totalMillis(toMillis(rootNanos))
                .truncated(treeTruncated)
                .build();
    }

    private static ProfileHotspotView toHotspotView(TableProfile stat) {
        return new ProfileHotspotView(stat.uri(), stat.name(), stat.kind(),
                toMillis(stat.selfNanos()), toMillis(stat.totalNanos()), stat.count());
    }

    /** Build a non-technical error view: cleaned message, the table that failed, and a technical drill-down. */
    private static @Nullable DebugError buildStackError(List<DebugFrame> frames, @Nullable Throwable error) {
        if (error == null) {
            return null;
        }
        DebugFrame failing = failingFrame(frames);
        return DebugError.builder()
                .summary(cleanSummary(error))
                .table(failing == null ? null : failing.getName())
                .location(failing == null ? null : locationLabel(failing.getLocation()))
                .type(rootCause(error).getClass().getSimpleName())
                .detail(stackTrace(error))
                .build();
    }

    /** The frame that failed: the deepest one marked with an error, otherwise the current (deepest) frame. */
    private static @Nullable DebugFrame failingFrame(List<DebugFrame> frames) {
        DebugFrame failing = null;
        for (DebugFrame frame : frames) {
            if (frame.getError() != null) {
                failing = frame;
            }
        }
        if (failing == null && !frames.isEmpty()) {
            failing = frames.getLast();
        }
        return failing;
    }

    private static @Nullable String locationLabel(@Nullable CurrentLocation location) {
        if (location == null) {
            return null;
        }
        return location.label() != null ? location.label() : location.ref();
    }

    /** Prefer the engine's cleaned OpenL message over the raw Java exception text. */
    private static String cleanSummary(Throwable error) {
        Throwable cause = Objects.requireNonNullElse(error.getCause(), error);
        for (OpenLMessage message : OpenLMessagesUtils.newErrorMessages(cause)) {
            if (message.getSummary() != null && !message.getSummary().isBlank()) {
                return message.getSummary();
            }
        }
        Throwable root = rootCause(error);
        return Objects.requireNonNullElse(root.getMessage(), root.getClass().getSimpleName());
    }

    private static Throwable rootCause(Throwable error) {
        return Objects.requireNonNullElse(ExceptionUtils.getRootCause(error), error);
    }

    private static String stackTrace(Throwable error) {
        String trace = ExceptionUtils.getStackTrace(error);
        return trace.length() > MAX_DETAIL ? trace.substring(0, MAX_DETAIL) + "…" : trace;
    }

    private static @Nullable DebugLocationView toLocationView(@Nullable CurrentLocation location) {
        if (location == null) {
            return null;
        }
        return DebugLocationView.builder()
                .kind(location.kind())
                .row(location.row() < 0 ? null : location.row())
                .column(location.column() < 0 ? null : location.column())
                .ref(location.ref())
                .label(location.label())
                .build();
    }

    /** Freeze a frame's variables. Must be called while the session is suspended. */
    public DebugFrameVariables freezeVariables(DebugFrame frame, @Nullable ClassLoader classLoader, boolean includeSchema) {
        var previous = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            var clones = new IdentityHashMap<Object, Object>();
            return DebugFrameVariables.builder()
                    .parameters(freezeParameters(frame, clones, includeSchema))
                    .context(freezeContext(frame, clones, includeSchema))
                    .result(freezeResult(frame, clones, includeSchema))
                    .steps(freezeSteps(frame, clones, includeSchema))
                    .gridColumns(gridNames(frame, true))
                    .gridRows(gridNames(frame, false))
                    .decision(decisionFor(frame))
                    .ruleNames(ruleNamesFor(frame))
                    .errors(buildErrors(frame))
                    .build();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private List<ParameterValue> freezeParameters(DebugFrame frame, Map<Object, Object> clones, boolean includeSchema) {
        if (!(frame.getSource() instanceof ExecutableRulesMethod method)) {
            return List.of();
        }
        var signature = method.getSignature();
        var params = frame.getParams();
        var count = Math.min(params.length, signature.getNumberOfParameters());
        var result = new ArrayList<ParameterValue>(count);
        for (var i = 0; i < count; i++) {
            var param = new ParameterWithValueDeclaration(
                    signature.getParameterName(i),
                    safeClone(params[i], clones, !frame.isCompleted()),
                    signature.getParameterType(i));
            result.add(buildParameterValue(param, true, includeSchema));
        }
        return result;
    }

    private @Nullable ParameterValue freezeContext(DebugFrame frame, Map<Object, Object> clones, boolean includeSchema) {
        if (frame.getContext() == null) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("context", safeClone(frame.getContext(), clones, !frame.isCompleted()));
        return buildParameterValue(param, false, includeSchema);
    }

    private List<StepValueView> freezeSteps(DebugFrame frame, Map<Object, Object> clones, boolean includeSchema) {
        if (frame.getSource() instanceof Spreadsheet spreadsheet) {
            return spreadsheetSteps(frame, spreadsheet, clones, includeSchema);
        }
        // Non-spreadsheet frames: just the executed sub-steps.
        List<DebugFrame.ExecutedStep> executed = frame.getExecutedSteps();
        var result = new ArrayList<StepValueView>(executed.size());
        for (DebugFrame.ExecutedStep step : executed) {
            String name = step.label() != null ? step.label() : step.ref();
            var param = new ParameterWithValueDeclaration(name, safeClone(step.value(), clones, !frame.isCompleted()));
            result.add(StepValueView.builder()
                    .ref(step.ref())
                    .label(step.label())
                    .status(StepStatus.EXECUTED)
                    .value(buildParameterValue(param, true, includeSchema))
                    .build());
        }
        return result;
    }

    /** All cells of a spreadsheet with their status (executed, current, pending) and executed values. */
    private List<StepValueView> spreadsheetSteps(DebugFrame frame, Spreadsheet spreadsheet, Map<Object, Object> clones, boolean includeSchema) {
        var executed = new HashMap<String, Object>();
        for (DebugFrame.ExecutedStep step : frame.getExecutedSteps()) {
            executed.put(step.ref(), step.value());
        }
        String currentRef = currentRef(frame);
        var steps = new ArrayList<StepValueView>();
        forEachCell(spreadsheet, TraceDebugMapper::isDisplayCell, cell -> {
            String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
            var builder = StepValueView.builder()
                    .ref(ref)
                    .label(SpreadsheetCellNames.of(spreadsheet, cell))
                    .cell(cellAddress(cell));
            if (executed.containsKey(ref)) {
                var param = new ParameterWithValueDeclaration(ref, safeClone(executed.get(ref), clones, !frame.isCompleted()), cell.getType());
                steps.add(builder.status(StepStatus.EXECUTED).value(buildParameterValue(param, true, includeSchema)).build());
            } else if (cell.isMethodCell()) {
                steps.add(builder.status(stepStatus(ref, Set.of(), currentRef)).build());
            } else {
                // A plain value or constant cell never runs — its static content is its value, so the
                // grid shows the whole table (descriptions included), like the legacy trace did.
                var param = new ParameterWithValueDeclaration(ref, cell.getValue(), cell.getType());
                steps.add(builder.status(StepStatus.EXECUTED).value(buildParameterValue(param, true, includeSchema)).build());
            }
        });
        return steps;
    }

    /**
     * The frame's sub-steps with status only (no values, no freeze), for the live-stack call tree.
     *
     * <p>Spreadsheet frames yield every cell, decision-table frames yield every rule, other frames yield
     * the executed sub-steps. Each carries {@code executed}, {@code current}, or {@code pending} so the
     * tree can render the whole stack in one pass without cloning any values.
     */
    static List<StepValueView> outlineSteps(DebugFrame frame, boolean withExecutedChildren) {
        var steps = withStepDurations(frame, baseSteps(frame));
        // Once the run has finished, the executed sub-calls hang off the (lazy) completed tree instead, so a
        // still-published root frame need not re-serialize them deep — that was the other half of a huge stack.
        return withExecutedChildren ? attachExecutedChildren(frame, steps) : steps;
    }

    /** Attach each executed step's own measured total time, looked up by its ref. */
    private static List<StepValueView> withStepDurations(DebugFrame frame, List<StepValueView> steps) {
        var durations = frame.getExecutedSteps().stream()
                .collect(Collectors.toMap(DebugFrame.ExecutedStep::ref, DebugFrame.ExecutedStep::durationNanos,
                        (first, second) -> second));
        if (durations.isEmpty()) {
            return steps;
        }
        return steps.stream()
                .map(step -> {
                    var nanos = durations.get(step.ref());
                    return nanos == null ? step : step.toBuilder().durationMillis(toMillis(nanos)).build();
                })
                .toList();
    }

    /** The frame's own sub-steps (cells or rules) with status, before any executed children are attached. */
    private static List<StepValueView> baseSteps(DebugFrame frame) {
        if (frame.getSource() instanceof Spreadsheet spreadsheet) {
            var executedRefs = executedRefs(frame);
            String currentRef = currentRef(frame);
            var steps = new ArrayList<StepValueView>();
            forEachCell(spreadsheet, TraceDebugMapper::isStepCell, cell -> {
                String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
                steps.add(StepValueView.builder()
                        .ref(ref)
                        .label(SpreadsheetCellNames.of(spreadsheet, cell))
                        .cell(cellAddress(cell))
                        .status(stepStatus(ref, executedRefs, currentRef))
                        .build());
            });
            return steps;
        }
        if (frame.getSource() instanceof IDecisionTable decisionTable) {
            return ruleOutline(decisionTable, firedRuleIndices(frame));
        }
        return frame.getExecutedSteps().stream()
                .map(step -> StepValueView.builder().ref(step.ref()).label(step.label()).status(StepStatus.EXECUTED).build())
                .toList();
    }

    /**
     * Attach each step's executed sub-calls (profiling mode) as children. Sub-calls whose calling step is
     * not itself listed — for example a decision-table action — are appended as their own steps, so no
     * executed branch is lost.
     */
    private static List<StepValueView> attachExecutedChildren(DebugFrame frame, List<StepValueView> steps) {
        Map<String, List<CallNode>> children = frame.getExecutedChildren();
        if (children.isEmpty()) {
            return steps;
        }
        var covered = new HashSet<String>();
        var result = new ArrayList<StepValueView>(steps.size());
        for (StepValueView step : steps) {
            covered.add(step.ref());
            List<CallNode> kids = children.get(step.ref());
            result.add(kids == null || kids.isEmpty()
                    ? step
                    : step.toBuilder().children(toCallNodeViews(kids)).childrenTotal(childrenTotalOf(kids)).build());
        }
        children.forEach((ref, kids) -> {
            if (!covered.contains(ref) && !kids.isEmpty()) {
                result.add(StepValueView.builder()
                        .ref(ref)
                        .status(StepStatus.EXECUTED)
                        .children(toCallNodeViews(kids))
                        .childrenTotal(childrenTotalOf(kids))
                        .build());
            }
        });
        return result;
    }

    /** Convert returned sub-calls to views, recursively — structure only, never values. */
    private static List<CallNodeView> toCallNodeViews(List<CallNode> nodes) {
        return nodes.stream().limit(MAX_TREE_CHILDREN).map(TraceDebugMapper::toCallNodeView).toList();
    }

    /** The full child count when the list was capped, so the client can show how many executions were omitted. */
    private static @Nullable Integer childrenTotalOf(List<CallNode> nodes) {
        return nodes.size() > MAX_TREE_CHILDREN ? nodes.size() : null;
    }

    private static CallNodeView toCallNodeView(CallNode node) {
        return toCallNodeView(node, false);
    }

    /**
     * Serialize a node one level deep: its own steps with timings and a child count per step, but not the
     * child sub-calls themselves. A step's children are fetched on demand ({@link #toChildrenView}), so a
     * huge run's executed tree is never serialized whole — only the branches the analyst opens.
     */
    static CallNodeView toShallowCallNodeView(CallNode node) {
        return toCallNodeView(node, true);
    }

    private static CallNodeView toCallNodeView(CallNode node, boolean shallow) {
        return nodeViewBase(node, node.steps().stream().map(step -> toStepView(step, shallow)).toList());
    }

    /**
     * Build a node view from its identity, timings, and an already-serialized step list — the part shared by
     * every serialization depth (shallow lazy, deep capped), so they never drift in the node metadata they
     * report. Self time is the node's own work: its total minus the time spent in the tables it called.
     * {@code childNanos} counts every sub-call it made — including ones the node cap dropped — so a truncated
     * node does not report the dropped children's time as its own Self; it falls back to summing the retained
     * children only for a node that carries no recorded {@code childNanos} (for example one synthesized in a test).
     */
    private static CallNodeView nodeViewBase(CallNode node, List<StepValueView> steps) {
        long childrenNanos = node.childNanos() > 0 ? node.childNanos()
                : sumDurations(node.steps().stream().flatMap(step -> step.children().stream()));
        return CallNodeView.builder()
                .uri(node.uri())
                .name(node.name())
                .instance(node.instance())
                .kind(node.kind())
                .durationMillis(toMillis(node.durationNanos()))
                .selfMillis(selfMillis(node.durationNanos(), childrenNanos))
                .steps(steps)
                .dispatch(node.dispatch())
                .refStep(node.refStep())
                .notRetained(node.notRetained() > 0 ? node.notRetained() : null)
                .build();
    }

    private static StepValueView toStepView(CallNode.Step step, boolean shallow) {
        var builder = stepViewBase(step);
        if (shallow) {
            // Children are fetched on demand; report only the count, so the client shows the step as
            // expandable and knows how many executions to page through.
            return builder.childrenTotal(step.children().isEmpty() ? null : step.children().size()).build();
        }
        return builder
                .children(step.children().isEmpty() ? null : toCallNodeViews(step.children()))
                .childrenTotal(childrenTotalOf(step.children()))
                .build();
    }

    /** A step's identity and timings, without its children — the part shared by every serialization depth. */
    private static StepValueView.StepValueViewBuilder stepViewBase(CallNode.Step step) {
        // A condition row (like a static cell) has no execution of its own — no timings.
        boolean condition = step.decision() == DecisionRow.MATCHED || step.decision() == DecisionRow.UNMATCHED;
        boolean noTimings = step.constant() || condition;
        return StepValueView.builder()
                .ref(step.ref())
                .label(step.label())
                .status(StepStatus.EXECUTED)
                // Static content has no execution of its own: no timings, and the flag lets a client
                // read the cell instead of trying to run to it.
                .constant(step.constant() ? Boolean.TRUE : null)
                .decision(step.decision())
                .durationMillis(noTimings ? null : toMillis(step.durationNanos()))
                .selfMillis(noTimings ? null
                        : selfMillis(step.durationNanos(), sumDurations(step.children().stream())));
    }

    /**
     * Serialize the whole executed tree in one payload for the business view: deep, with every step's
     * sub-calls inline, so the client browses it offline without paging. Bounded to {@link #MAX_FULL_TREE_NODES}
     * nodes — a branch beyond the budget is cut, and its step reports the full child count so the client can
     * mark how many executions are omitted. The advanced view keeps the shallow, lazily paged tree.
     */
    static CallNodeView toCappedTree(CallNode root) {
        return toCappedTree(root, MAX_FULL_TREE_NODES);
    }

    /** Same as {@link #toCappedTree(CallNode)} with an explicit node budget, so a test can force truncation. */
    static CallNodeView toCappedTree(CallNode root, int budget) {
        return toCappedNode(root, new int[]{budget});
    }

    private static CallNodeView toCappedNode(CallNode node, int[] budget) {
        return nodeViewBase(node, node.steps().stream().map(step -> toCappedStep(step, budget)).toList());
    }

    private static StepValueView toCappedStep(CallNode.Step step, int[] budget) {
        var builder = stepViewBase(step);
        List<CallNode> kids = step.children();
        if (kids.isEmpty()) {
            return builder.build();
        }
        // Serialize children deep, decrementing the shared budget, up to the per-step cap. Whatever the
        // budget cannot fit is omitted; the full count is reported so the client marks it truncated.
        int limit = Math.min(kids.size(), MAX_TREE_CHILDREN);
        List<CallNodeView> included = new ArrayList<>();
        for (int i = 0; i < limit && budget[0] > 0; i++) {
            budget[0]--;
            included.add(toCappedNode(kids.get(i), budget));
        }
        return builder
                .children(included.isEmpty() ? null : included)
                .childrenTotal(included.size() < kids.size() ? kids.size() : null)
                .build();
    }

    /**
     * The children of one step of one executed frame, one level deep, paged with {@code offset}/{@code limit}.
     * The frame is addressed by its {@code (uri, instance)} — unique per execution in the run — so a specific
     * loop iteration's sub-tree is reachable. Returns an empty page if the frame or step is no longer retained.
     */
    public static TreeChildrenView toChildrenView(@Nullable CallNode root, String uri, int instance, String stepRef,
                                                  int offset, int limit) {
        CallNode frame = root == null ? null : findNode(root, uri, instance);
        List<CallNode> children = frame == null ? List.of() : frame.steps().stream()
                .filter(step -> stepRef.equals(step.ref()))
                .findFirst()
                .map(CallNode.Step::children)
                .orElse(List.of());
        List<CallNodeView> page = children.stream()
                .skip(Math.max(0, offset))
                .limit(Math.max(1, limit))
                .map(TraceDebugMapper::toShallowCallNodeView)
                .toList();
        return new TreeChildrenView(page, children.size());
    }

    /** Depth-first search for the retained frame with the given {@code (uri, instance)}; references are skipped. */
    private static @Nullable CallNode findNode(CallNode node, String uri, int instance) {
        if (node.refStep() == null && node.instance() == instance && uri.equals(node.uri())) {
            return node;
        }
        for (CallNode.Step step : node.steps()) {
            for (CallNode child : step.children()) {
                CallNode found = findNode(child, uri, instance);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Total time of a frame that has already returned (for example after a step out), otherwise {@code null}. */
    private static @Nullable Double completedMillis(DebugFrame frame) {
        return frame.isCompleted() ? toMillis(frame.getDurationNanos()) : null;
    }

    /** Own time of a returned frame: its total minus the time spent in the tables it called. */
    private static @Nullable Double completedSelfMillis(DebugFrame frame) {
        if (!frame.isCompleted()) {
            return null;
        }
        var childrenNanos = sumDurations(frame.getExecutedChildren().values().stream().flatMap(List::stream));
        return selfMillis(frame.getDurationNanos(), childrenNanos);
    }

    /** Sum of the durations of returned sub-calls. */
    private static long sumDurations(Stream<CallNode> nodes) {
        return nodes.mapToLong(CallNode::durationNanos).sum();
    }

    /** Nanoseconds as fractional milliseconds. */
    private static double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    /** Own time: a total minus the time spent in the tables it called, clamped at zero. */
    private static double selfMillis(long totalNanos, long childrenNanos) {
        return toMillis(Math.max(0, totalNanos - childrenNanos));
    }

    /**
     * Every rule of a decision table as a step. A decision-table frame on the live stack is always
     * mid-firing, so the rule whose action is running is the current one — and the called sub-table nests
     * under it. The rest are still pending and can be armed for a run-to.
     */
    static List<StepValueView> ruleOutline(IDecisionTable decisionTable, int[] firedRuleIndices) {
        var fired = Arrays.stream(firedRuleIndices)
                .mapToObj(decisionTable::getRuleName)
                .collect(Collectors.toSet());
        return ruleNames(decisionTable).stream()
                .map(name -> StepValueView.builder()
                        .ref(name)
                        .label(name)
                        .status(fired.contains(name) ? StepStatus.CURRENT : StepStatus.PENDING)
                        .build())
                .toList();
    }

    /** An input a step's formula consumed, ranked so the list reads steps → parameters → constants. */
    private record StepInput(int rank, int order, String name, @Nullable Object value, @Nullable IOpenClass type) {
    }

    /**
     * A focused spreadsheet step, self-contained: the values its formula consumed, the step's own returned
     * value, and the A1 address of its cell.
     *
     * <p>Everything the step panel shows, in one payload — a step click need not also fetch the frame's full
     * variables that the panel never renders. Resolved from the frame's recorded values; nothing is
     * re-evaluated.
     */
    public StepInputsView freezeStepInputs(DebugFrame frame, String stepRef,
                                           @Nullable ClassLoader classLoader, boolean includeSchema) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            if (!(frame.getSource() instanceof Spreadsheet spreadsheet)) {
                return new StepInputsView(List.of(), null, null);
            }
            SpreadsheetCell cell = displayCell(spreadsheet, stepRef);
            if (cell == null) {
                return new StepInputsView(List.of(), null, null);
            }
            Map<String, Object> executed = new HashMap<>();
            for (DebugFrame.ExecutedStep step : frame.getExecutedSteps()) {
                executed.put(step.ref(), step.value());
            }
            Map<Object, Object> clones = new IdentityHashMap<>();
            List<ParameterValue> inputs = cell.getMethod() instanceof CompositeMethod composite
                    ? formulaInputs(composite, frame, spreadsheet, executed, clones, includeSchema)
                    : List.of();
            ParameterValue result = stepResult(frame, cell, stepRef, executed, clones, includeSchema);
            return new StepInputsView(inputs, result, cellAddress(cell));
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /**
     * The values a step's formula consumed, named as the formula writes them: sibling steps such as
     * {@code $LimitIndex}, the table's own parameters, fields of a parameter opened into the table's
     * scope such as {@code currentFinancialData}, and module constants such as {@code MaxLimit}.
     *
     * <p>Resolved from the compiled cell's binding dependencies against the frame's recorded values —
     * nothing is re-evaluated. A sibling step that has not executed yet is omitted, and so is anything
     * the recorded data cannot resolve (for example a field of another step's result).
     */
    private List<ParameterValue> formulaInputs(CompositeMethod composite, DebugFrame frame, Spreadsheet spreadsheet,
                                               Map<String, Object> executed, Map<Object, Object> clones,
                                               boolean includeSchema) {
        var dependencies = new RulesBindingDependencies();
        composite.updateDependency(dependencies);
        List<IOpenField> fields = new ArrayList<>(dependencies.getFieldsMap().values());
        List<StepInput> inputs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // A field picked from another step's result ($Cell.$Field) is the precise input: list it as
        // the dotted name with the field's value, and drop the bare result the formula only reached
        // through.
        Set<IOpenField> narrowed = new HashSet<>();
        for (IOpenField field : fields) {
            if (field instanceof CustomSpreadsheetResultField resultField) {
                StepInput input = resultFieldInput(resultField, fields, executed, narrowed);
                if (input != null && seen.add(input.name())) {
                    inputs.add(input);
                }
            }
        }
        // A field read explicitly off a parameter (policy.census) is the precise input: list it as the dotted
        // name with the field's value, and drop the bare parameter the formula only reached through.
        for (IOpenField field : fields) {
            if (field instanceof DatatypeOpenField datatypeField) {
                StepInput input = parameterFieldInput(datatypeField, fields, frame, spreadsheet, narrowed);
                if (input != null && seen.add(input.name())) {
                    inputs.add(input);
                }
            }
        }
        for (IOpenField field : fields) {
            if (field instanceof CustomSpreadsheetResultField || narrowed.contains(field)) {
                continue;
            }
            for (StepInput input : resolveStepInputs(field, frame, spreadsheet, executed)) {
                if (seen.add(input.name())) {
                    inputs.add(input);
                }
            }
        }
        return inputs.stream()
                .sorted(Comparator.comparingInt(StepInput::rank)
                        .thenComparingInt(StepInput::order)
                        .thenComparing(StepInput::name))
                .map(input -> buildParameterValue(new ParameterWithValueDeclaration(input.name(),
                        safeClone(input.value(), clones, !frame.isCompleted()), input.type()), true, includeSchema))
                .toList();
    }

    /**
     * The step's own returned value, named {@code return}: the recorded value of an executed formula cell,
     * or the static content of a plain value or constant cell. {@code null} for a formula cell that has not
     * run yet — there is no value to show.
     */
    private @Nullable ParameterValue stepResult(DebugFrame frame, SpreadsheetCell cell, String stepRef,
                                                Map<String, Object> executed, Map<Object, Object> clones,
                                                boolean includeSchema) {
        Object value;
        if (executed.containsKey(stepRef)) {
            value = executed.get(stepRef);
        } else if (cell.isMethodCell()) {
            return null;
        } else {
            value = cell.getValue();
        }
        var param = new ParameterWithValueDeclaration("return",
                safeClone(value, clones, !frame.isCompleted()), cell.getType());
        return buildParameterValue(param, true, includeSchema);
    }

    private List<StepInput> resolveStepInputs(IOpenField field, DebugFrame frame, Spreadsheet spreadsheet,
                                              Map<String, Object> executed) {
        if (field instanceof SpreadsheetRangeField range) {
            // A cell range ($First:$Last) reads as the individual steps it spans, like the tree shows it.
            List<StepInput> inputs = new ArrayList<>();
            for (int row = range.getStartRowIndex(); row <= range.getEndRowIndex(); row++) {
                for (int column = range.getStartColumnIndex(); column <= range.getEndColumnIndex(); column++) {
                    StepInput input = rangeCellInput(spreadsheet, executed, row, column);
                    if (input != null) {
                        inputs.add(input);
                    }
                }
            }
            return inputs;
        }
        StepInput single = resolveStepInput(field, frame, spreadsheet, executed);
        return single == null ? List.of() : List.of(single);
    }

    /**
     * A field read from another step's result, e.g. {@code $BalanceQualityIndexCalculation.$Value$BalanceQualityIndex}:
     * pair the field with the sibling step of its declaring result type, read the field off that step's
     * recorded value, and mark the bare step as narrowed so it is not listed on top of its field.
     */
    private static @Nullable StepInput resultFieldInput(CustomSpreadsheetResultField field, List<IOpenField> fields,
                                                        Map<String, Object> executed, Set<IOpenField> narrowed) {
        for (IOpenField candidate : fields) {
            if (!(candidate instanceof SpreadsheetCellField cellField)
                    || !cellField.getType().getName().equals(field.getDeclaringClass().getName())) {
                continue;
            }
            SpreadsheetCell cell = cellField.getCell();
            String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
            if (!executed.containsKey(ref)) {
                return null;
            }
            try {
                Object result = executed.get(ref);
                Object value = result == null ? null : field.get(result, null);
                narrowed.add(cellField);
                return new StepInput(0, gridOrder(cell.getRowIndex(), cell.getColumnIndex()),
                        cellField.getName() + "." + field.getName(), value, field.getType());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** A cell's position key for a table-shaped ordering: row-major, with room for many columns per row. */
    private static int gridOrder(int row, int column) {
        return row * 10_000 + column;
    }

    /** One executed cell of a referenced range, named by its OpenL cell name. */
    private static @Nullable StepInput rangeCellInput(Spreadsheet spreadsheet, Map<String, Object> executed,
                                                      int row, int column) {
        SpreadsheetCell[][] cells = spreadsheet.getCells();
        SpreadsheetCell cell = row < cells.length && column < cells[row].length ? cells[row][column] : null;
        if (cell == null || !isStepCell(cell)) {
            return null;
        }
        String ref = CurrentLocation.cellRef(row, column);
        if (!executed.containsKey(ref)) {
            return null;
        }
        return new StepInput(0, gridOrder(row, column), SpreadsheetCellNames.of(spreadsheet, cell),
                executed.get(ref), cell.getType());
    }

    private @Nullable StepInput resolveStepInput(IOpenField field, DebugFrame frame, Spreadsheet spreadsheet,
                                                 Map<String, Object> executed) {
        if (field instanceof SpreadsheetCellField cellField) {
            SpreadsheetCell used = cellField.getCell();
            String ref = CurrentLocation.cellRef(used.getRowIndex(), used.getColumnIndex());
            // A referenced step that has not executed yet has no recorded value to show.
            if (!executed.containsKey(ref)) {
                return null;
            }
            int order = gridOrder(used.getRowIndex(), used.getColumnIndex());
            return new StepInput(0, order, field.getName(), executed.get(ref), cellField.getType());
        }
        if (field instanceof ILocalVar) {
            // The table's own parameter used by name (e.g. `bank`).
            IMethodSignature signature = spreadsheet.getSignature();
            Object[] params = frame.getParams();
            int count = Math.min(params.length, signature.getNumberOfParameters());
            for (int i = 0; i < count; i++) {
                if (field.getName().equals(signature.getParameterName(i))) {
                    return new StepInput(1, i, field.getName(), params[i], signature.getParameterType(i));
                }
            }
            return null;
        }
        if (field instanceof ConstantOpenField constant) {
            return new StepInput(3, 0, constant.getName(), constant.getValue(), constant.getType());
        }
        if (field instanceof OpenFieldDelegator delegator) {
            return parameterScopeInput(delegator, frame, spreadsheet);
        }
        return null;
    }

    /**
     * A field of a parameter opened into the table's scope (e.g. {@code currentFinancialData} resolved
     * as a field of the {@code bank} parameter): read it from that parameter's recorded value.
     */
    private static @Nullable StepInput parameterScopeInput(OpenFieldDelegator field, DebugFrame frame,
                                                           Spreadsheet spreadsheet) {
        IOpenClass declaring = field.getDeclaringClass();
        if (declaring == null) {
            return null;
        }
        IMethodSignature signature = spreadsheet.getSignature();
        Object[] params = frame.getParams();
        int count = Math.min(params.length, signature.getNumberOfParameters());
        for (int i = 0; i < count; i++) {
            if (declaring.isAssignableFrom(signature.getParameterType(i))) {
                try {
                    Object value = params[i] == null ? null : field.getDelegate().get(params[i], null);
                    return new StepInput(2, i, field.getName(), value, field.getType());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * A field read explicitly off a parameter, e.g. {@code policy.census}: pair the datatype field with the
     * parameter of its declaring type, read the field off that parameter's recorded value, and name it with
     * the dotted path. The bare parameter, present only as the root of the access, is narrowed so it is not
     * listed on top of its field.
     */
    private static @Nullable StepInput parameterFieldInput(DatatypeOpenField field, List<IOpenField> fields,
                                                           DebugFrame frame, Spreadsheet spreadsheet,
                                                           Set<IOpenField> narrowed) {
        IOpenClass declaring = field.getDeclaringClass();
        if (declaring == null) {
            return null;
        }
        IMethodSignature signature = spreadsheet.getSignature();
        Object[] params = frame.getParams();
        int count = Math.min(params.length, signature.getNumberOfParameters());
        for (int i = 0; i < count; i++) {
            if (!declaring.isAssignableFrom(signature.getParameterType(i))) {
                continue;
            }
            String parameter = signature.getParameterName(i);
            fields.stream()
                    .filter(candidate -> candidate instanceof ILocalVar && parameter.equals(candidate.getName()))
                    .forEach(narrowed::add);
            try {
                Object value = params[i] == null ? null : field.get(params[i], null);
                return new StepInput(2, i, parameter + "." + field.getName(), value, field.getType());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * The displayable cell of a spreadsheet with the given {@code RnCm} reference, or {@code null}. Matches
     * an executable step, a plain value, or a constant cell — so a focused static cell resolves too, not
     * only formulas.
     */
    private static @Nullable SpreadsheetCell displayCell(Spreadsheet spreadsheet, String ref) {
        int[] rowCol = CurrentLocation.parseCellRef(ref);
        if (rowCol == null) {
            return null;
        }
        SpreadsheetCell[][] cells = spreadsheet.getCells();
        int row = rowCol[0];
        int col = rowCol[1];
        SpreadsheetCell cell = row >= 0 && row < cells.length && col >= 0 && col < cells[row].length
                ? cells[row][col] : null;
        return isDisplayCell(cell) ? cell : null;
    }

    /**
     * The A1 address of a step cell's source in the raw table, matching the addresses of the Tables API
     * grid, so a client can point at the step within its table.
     */
    private static @Nullable String cellAddress(SpreadsheetCell cell) {
        var source = cell.getSourceCell();
        if (source == null) {
            return null;
        }
        var region = source.getAbsoluteRegion();
        return XlsUtil.xlsCellPresentation(region.getLeft(), region.getTop());
    }

    /**
     * Apply an action to every cell the filter accepts, in grid order. Callers pass {@link #isStepCell} for
     * the executable steps only, or {@link #isDisplayCell} to also include the plain value and constant cells
     * so a rendered grid shows the whole table, not only what can run.
     */
    private static void forEachCell(Spreadsheet spreadsheet, Predicate<SpreadsheetCell> filter,
                                    Consumer<SpreadsheetCell> action) {
        for (SpreadsheetCell[] row : spreadsheet.getCells()) {
            for (SpreadsheetCell cell : row) {
                if (filter.test(cell)) {
                    action.accept(cell);
                }
            }
        }
    }

    /**
     * A displayable cell: an executable step, a plain value, or a constant. Section-title dividers and empty
     * cells are neither shown nor addressable. Broader than {@link #isStepCell} — a value cell is displayed
     * but never runs.
     */
    private static boolean isDisplayCell(@Nullable SpreadsheetCell cell) {
        if (cell == null) {
            return false;
        }
        SpreadsheetCellType type = cell.getSpreadsheetCellType();
        return type == SpreadsheetCellType.METHOD || type == SpreadsheetCellType.VALUE
                || type == SpreadsheetCellType.CONSTANT;
    }

    /**
     * A real spreadsheet step: a cell with a formula that is actually evaluated. Only these are invoked,
     * timed, and recorded during a trace. Value cells (plain literals), constant cells, section-title
     * dividers, and empty cells are static data or labels — they never execute, so they are not steps.
     */
    private static boolean isStepCell(@Nullable SpreadsheetCell cell) {
        return cell != null && cell.isMethodCell();
    }

    /** Classify a step: already executed, currently executing, or still pending. */
    private static StepStatus stepStatus(String ref, Set<String> executedRefs, @Nullable String currentRef) {
        if (executedRefs.contains(ref)) {
            return StepStatus.EXECUTED;
        }
        return ref.equals(currentRef) ? StepStatus.CURRENT : StepStatus.PENDING;
    }

    private static Set<String> executedRefs(DebugFrame frame) {
        return frame.getExecutedSteps().stream().map(DebugFrame.ExecutedStep::ref).collect(Collectors.toSet());
    }

    private static @Nullable String currentRef(DebugFrame frame) {
        var location = frame.getLocation();
        return location == null ? null : location.ref();
    }

    /** Spreadsheet column or row names, so the UI can lay the steps out as a grid like the source table. */
    private static @Nullable List<String> gridNames(DebugFrame frame, boolean columns) {
        if (!(frame.getSource() instanceof Spreadsheet spreadsheet)) {
            return null;
        }
        String[] names = columns ? spreadsheet.getColumnNames() : spreadsheet.getRowNames();
        return Arrays.stream(names).map(name -> name == null ? "" : name).toList();
    }

    /** Every distinct rule name of a decision-table frame, so any rule can be armed; {@code null} otherwise. */
    private static @Nullable List<String> ruleNamesFor(DebugFrame frame) {
        return frame.getSource() instanceof IDecisionTable decisionTable ? ruleNames(decisionTable) : null;
    }

    /** Every distinct rule name of a decision table, in rule order. */
    static List<String> ruleNames(IDecisionTable decisionTable) {
        return IntStream.range(0, decisionTable.getNumberOfRules())
                .mapToObj(decisionTable::getRuleName)
                .distinct()
                .toList();
    }

    private @Nullable ParameterValue freezeResult(DebugFrame frame, Map<Object, Object> clones, boolean includeSchema) {
        if (!frame.isCompleted() || frame.getResult() == null
                || !(frame.getSource() instanceof ExecutableRulesMethod method)) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("return", safeClone(frame.getResult(), clones, false), method.getType());
        return buildParameterValue(param, true, includeSchema);
    }

    /**
     * Freeze a value for later inspection. A suspended frame's values are live and may change once the worker
     * resumes, so they are deep-cloned. A settled frame (completed or failed) will not run again, so its values
     * are already stable — cloning them is skipped, avoiding a deep copy (and its heap blow-up) of a huge result.
     */
    private static Object safeClone(Object value, Map<Object, Object> clones, boolean freeze) {
        if (value == null || !freeze) {
            return value;
        }
        try {
            return Cloner.clone(value, clones);
        } catch (Exception e) {
            log.debug("Failed to freeze a value, using the live reference", e);
            return value;
        }
    }

    private List<MessageDescription> buildErrors(DebugFrame frame) {
        var error = frame.getError();
        if (error == null) {
            return List.of();
        }
        Throwable cause = Objects.requireNonNullElse(error.getCause(), error);
        var result = new ArrayList<MessageDescription>();
        for (OpenLMessage message : OpenLMessagesUtils.newErrorMessages(cause)) {
            result.add(new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()));
        }
        return result;
    }

    /** Decision-table outcome explanation, or {@code null} for non-decision-table frames. */
    private static @Nullable DecisionView decisionFor(DebugFrame frame) {
        if (!(frame.getSource() instanceof IDecisionTable decisionTable)) {
            return null;
        }
        return buildDecision(decisionTable, frame.getConditionChecks(), firedRuleIndices(frame));
    }

    private static int[] firedRuleIndices(DebugFrame frame) {
        return frame.getCurrentStep() instanceof ActionInvoker invoker ? invoker.getRules() : new int[0];
    }

    /**
     * Build the plain-language decision outcome from the rules that fired and the conditions evaluated.
     * Mirrors the green/red table highlight: one entry per condition cell that was checked, so the
     * explanation never claims more than the engine actually evaluated.
     */
    static @Nullable DecisionView buildDecision(IDecisionTable decisionTable, List<ConditionCheck> checks,
                                                int[] firedRules) {
        if (checks.isEmpty() && firedRules.length == 0) {
            return null;
        }
        List<String> fired = Arrays.stream(firedRules).mapToObj(decisionTable::getRuleName).toList();
        var conditions = new ArrayList<DecisionConditionView>();
        for (ConditionCheck check : checks) {
            if (!(check.condition() instanceof IBaseCondition condition)) {
                continue;
            }
            var name = condition.getName();
            for (int rule : check.rules()) {
                conditions.add(new DecisionConditionView(name, decisionTable.getRuleName(rule), check.successful()));
            }
        }
        return new DecisionView(fired, conditions);
    }

    /** Build a parameter value, registering large values for lazy retrieval. */
    public ParameterValue buildParameterValue(ParameterWithValueDeclaration param, boolean preferLazy,
                                              boolean includeSchema) {
        var type = param.getType();
        var rawValue = param.getValue();
        var description = type != null ? type.getDisplayName(INamedThing.SHORT) : null;
        var isSimple = type != null && type.isSimple();
        var builder = ParameterValue.builder()
                .name(param.getName())
                .description(description)
                // The schema is generated only on request: it is derived from the value's type via a recursive
                // JSON-schema pass that is expensive for a large spreadsheet result, and no Studio client reads it.
                .schema(includeSchema ? generateSchema(type) : null);
        if (preferLazy && rawValue != null && !isSimple) {
            return builder
                    .lazy(true)
                    .parameterId(parameterRegistry.register(param))
                    .build();
        }
        return builder
                .lazy(false)
                .value(serializeValue(rawValue, type))
                .build();
    }

    private @Nullable ObjectNode generateSchema(@Nullable IOpenClass type) {
        if (type == null || type.getInstanceClass() == null) {
            return null;
        }
        var clazz = type instanceof CustomSpreadsheetResultOpenClass csr ? csr.getBeanClass() : type.getInstanceClass();
        return SafeSchemaGenerator.generate(schemaGenerator, clazz);
    }

    private @Nullable JsonNode serializeValue(@Nullable Object value, @Nullable IOpenClass type) {
        if (value == null) {
            return null;
        }
        try {
            Class<?> toType = type instanceof CustomSpreadsheetResultOpenClass csr ? csr.getBeanClass() : null;
            Object converted = SpreadsheetResult.convertSpreadsheetResult(value, toType, type, null);
            return objectMapper.valueToTree(converted);
        } catch (Exception e) {
            log.debug("Failed to serialize a frozen value", e);
            return null;
        }
    }
}
