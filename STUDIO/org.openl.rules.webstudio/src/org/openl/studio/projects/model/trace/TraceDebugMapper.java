package org.openl.studio.projects.model.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cloner.Cloner;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.webstudio.web.trace.debug.CallNode;
import org.openl.rules.webstudio.web.trace.debug.ConditionCheck;
import org.openl.rules.webstudio.web.trace.debug.CurrentLocation;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.rules.webstudio.web.trace.debug.FrameKind;
import org.openl.rules.webstudio.web.trace.debug.SpreadsheetCellNames;
import org.openl.rules.webstudio.web.trace.debug.WatchCapture;
import org.openl.studio.config.SafeSchemaGenerator;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;

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

    /** Default number of hotspots in the profile overview when the caller does not ask for a specific size. */
    public static final int DEFAULT_PROFILE_TOP = 20;

    /** Map the live stack (root to current frame) to a stack view. */
    public static DebugStackView toStackView(DebugStatus status, List<DebugFrame> frames, @Nullable Throwable error) {
        return toStackView(status, frames, error, null);
    }

    /** Map the live stack to a stack view, plus the completed executed tree once the trace has finished. */
    public static DebugStackView toStackView(DebugStatus status, List<DebugFrame> frames, @Nullable Throwable error,
                                             @Nullable CallNode completedTree) {
        return toStackView(status, frames, error, completedTree, StackRenderOptions.FULL);
    }

    /**
     * Map the live stack to a stack view, shaped by {@code options}. Once the trace has finished in
     * profiling mode the completed tree is available; {@code includeTree} embeds it in full, and a bounded
     * profile overview (the slowest tables) is always attached so a large run can be understood without it.
     * In {@code compact} mode only the active frame carries its sub-steps, so a step no longer re-sends
     * every frame's steps.
     */
    public static DebugStackView toStackView(DebugStatus status, List<DebugFrame> frames, @Nullable Throwable error,
                                             @Nullable CallNode completedTree, StackRenderOptions options) {
        List<DebugFrameView> views = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            DebugFrame frame = frames.get(i);
            boolean active = i == frames.size() - 1;
            views.add(DebugFrameView.builder()
                    .index(i)
                    .depth(frame.getDepth())
                    .uri(frame.getUri())
                    .tableId(TableUtils.makeTableId(frame.getUri()))
                    .name(frame.getName())
                    .kind(frame.getKind())
                    .location(toLocationView(frame.getLocation()))
                    .active(active)
                    .completed(frame.isCompleted())
                    .error(frame.getError() != null)
                    .steps(options.compact() && !active ? null : outlineSteps(frame))
                    .durationMillis(completedMillis(frame))
                    .selfMillis(completedSelfMillis(frame))
                    .dispatch(frame.getDispatch())
                    .build());
        }
        return DebugStackView.builder()
                .status(status)
                .frames(views)
                .error(buildStackError(frames, error))
                .tree(completedTree == null || !options.includeTree() ? null : toCallNodeView(completedTree))
                .profile(completedTree == null ? null : buildProfileSummary(completedTree, options.profileTop()))
                .build();
    }

    /**
     * Group watched-cell captures into series: one per cell (scoped to its table), points in execution
     * order. Captures already arrive in execution order, so the points need no re-sorting.
     */
    public static WatchView toWatchView(List<WatchCapture> captures, boolean truncated) {
        Map<String, List<WatchPointView>> pointsByKey = new LinkedHashMap<>();
        Map<String, WatchCapture> firstByKey = new LinkedHashMap<>();
        for (WatchCapture capture : captures) {
            String key = capture.name() + ' ' + capture.tableUri();
            pointsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(toWatchPoint(capture));
            firstByKey.putIfAbsent(key, capture);
        }
        List<WatchSeriesView> series = new ArrayList<>(pointsByKey.size());
        pointsByKey.forEach((key, points) -> {
            WatchCapture first = firstByKey.get(key);
            series.add(WatchSeriesView.builder()
                    .name(first.name())
                    .table(first.table())
                    .tableUri(first.tableUri())
                    .points(points)
                    .build());
        });
        return WatchView.builder().series(series).truncated(truncated).build();
    }

    private static WatchPointView toWatchPoint(WatchCapture capture) {
        return WatchPointView.builder()
                .instance(capture.instance())
                .label(capture.table() + " #" + (capture.instance() + 1))
                .path(capture.path())
                .ref(capture.ref())
                .value(capture.value())
                .build();
    }

    /**
     * Fold the executed call tree into a bounded hotspots overview: every invocation of the same table
     * aggregated, keeping only the slowest {@code top} by own time. Constant-sized regardless of run size.
     */
    static ProfileSummaryView buildProfileSummary(CallNode root, int top) {
        Map<String, Hotspot> byUri = new HashMap<>();
        int nodeCount = accumulateHotspots(root, byUri, 0);
        List<ProfileHotspotView> hotspots = byUri.values().stream()
                .sorted(Comparator.comparingLong(Hotspot::selfNanos).reversed())
                .limit(Math.max(1, top))
                .map(Hotspot::toView)
                .toList();
        return ProfileSummaryView.builder()
                .hotspots(hotspots)
                .distinctTables(byUri.size())
                .nodeCount(nodeCount)
                .totalMillis(toMillis(root.durationNanos()))
                .truncated(byUri.size() > hotspots.size())
                .build();
    }

    /** Add a node's own time to its table's hotspot and recurse into the tables its steps called. */
    private static int accumulateHotspots(CallNode node, Map<String, Hotspot> byUri, int nodeCount) {
        if (node.refStep() != null) {
            // A reference to a step that ran elsewhere: no time of its own, so it is not an invocation.
            return nodeCount;
        }
        long childrenNanos = sumDurations(node.steps().stream().flatMap(step -> step.children().stream()));
        byUri.computeIfAbsent(node.uri(), uri -> new Hotspot(uri, node.name(), node.kind()))
                .add(node.durationNanos(), Math.max(0, node.durationNanos() - childrenNanos));
        int count = nodeCount + 1;
        for (CallNode.Step step : node.steps()) {
            for (CallNode child : step.children()) {
                count = accumulateHotspots(child, byUri, count);
            }
        }
        return count;
    }

    /** Mutable accumulator for one table's aggregated profiling time across all its invocations. */
    private static final class Hotspot {
        private final String uri;
        private final String name;
        private final FrameKind kind;
        private long totalNanos;
        private long selfNanos;
        private int count;

        private Hotspot(String uri, String name, FrameKind kind) {
            this.uri = uri;
            this.name = name;
            this.kind = kind;
        }

        private void add(long totalNanos, long selfNanos) {
            this.totalNanos += totalNanos;
            this.selfNanos += selfNanos;
            this.count++;
        }

        private long selfNanos() {
            return selfNanos;
        }

        private ProfileHotspotView toView() {
            return new ProfileHotspotView(uri, name, kind, toMillis(selfNanos), toMillis(totalNanos), count);
        }
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
            failing = frames.get(frames.size() - 1);
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
    public DebugFrameVariables freezeVariables(DebugFrame frame, @Nullable ClassLoader classLoader) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            Map<Object, Object> clones = new IdentityHashMap<>();
            return DebugFrameVariables.builder()
                    .parameters(freezeParameters(frame, clones))
                    .context(freezeContext(frame, clones))
                    .result(freezeResult(frame, clones))
                    .steps(freezeSteps(frame, clones))
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

    private List<ParameterValue> freezeParameters(DebugFrame frame, Map<Object, Object> clones) {
        if (!(frame.getSource() instanceof ExecutableRulesMethod method)) {
            return Collections.emptyList();
        }
        IMethodSignature signature = method.getSignature();
        Object[] params = frame.getParams();
        int count = Math.min(params.length, signature.getNumberOfParameters());
        List<ParameterValue> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var param = new ParameterWithValueDeclaration(
                    signature.getParameterName(i),
                    safeClone(params[i], clones),
                    signature.getParameterType(i));
            result.add(buildParameterValue(param, true));
        }
        return result;
    }

    private @Nullable ParameterValue freezeContext(DebugFrame frame, Map<Object, Object> clones) {
        if (frame.getContext() == null) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("context", safeClone(frame.getContext(), clones));
        return buildParameterValue(param, false);
    }

    private List<StepValueView> freezeSteps(DebugFrame frame, Map<Object, Object> clones) {
        if (frame.getSource() instanceof Spreadsheet spreadsheet) {
            return spreadsheetSteps(frame, spreadsheet, clones);
        }
        // Non-spreadsheet frames: just the executed sub-steps.
        List<DebugFrame.ExecutedStep> executed = frame.getExecutedSteps();
        List<StepValueView> result = new ArrayList<>(executed.size());
        for (DebugFrame.ExecutedStep step : executed) {
            String name = step.label() != null ? step.label() : step.ref();
            var param = new ParameterWithValueDeclaration(name, safeClone(step.value(), clones));
            result.add(StepValueView.builder()
                    .ref(step.ref())
                    .label(step.label())
                    .status(StepStatus.EXECUTED)
                    .value(buildParameterValue(param, true))
                    .build());
        }
        return result;
    }

    /** All cells of a spreadsheet with their status (executed, current, pending) and executed values. */
    private List<StepValueView> spreadsheetSteps(DebugFrame frame, Spreadsheet spreadsheet, Map<Object, Object> clones) {
        Map<String, Object> executed = new HashMap<>();
        for (DebugFrame.ExecutedStep step : frame.getExecutedSteps()) {
            executed.put(step.ref(), step.value());
        }
        String currentRef = currentRef(frame);
        List<StepValueView> steps = new ArrayList<>();
        forEachCell(spreadsheet, cell -> {
            String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
            var builder = StepValueView.builder().ref(ref).label(SpreadsheetCellNames.of(spreadsheet, cell));
            if (executed.containsKey(ref)) {
                var param = new ParameterWithValueDeclaration(ref, safeClone(executed.get(ref), clones), cell.getType());
                steps.add(builder.status(StepStatus.EXECUTED).value(buildParameterValue(param, true)).build());
            } else {
                steps.add(builder.status(stepStatus(ref, Collections.emptySet(), currentRef)).build());
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
    static List<StepValueView> outlineSteps(DebugFrame frame) {
        return attachExecutedChildren(frame, withStepDurations(frame, baseSteps(frame)));
    }

    /** Attach each executed step's own measured total time, looked up by its ref. */
    private static List<StepValueView> withStepDurations(DebugFrame frame, List<StepValueView> steps) {
        Map<String, Long> durations = frame.getExecutedSteps().stream()
                .collect(Collectors.toMap(DebugFrame.ExecutedStep::ref, DebugFrame.ExecutedStep::durationNanos,
                        (first, second) -> second));
        if (durations.isEmpty()) {
            return steps;
        }
        return steps.stream()
                .map(step -> {
                    Long nanos = durations.get(step.ref());
                    return nanos == null ? step : step.toBuilder().durationMillis(toMillis(nanos)).build();
                })
                .toList();
    }

    /** The frame's own sub-steps (cells or rules) with status, before any executed children are attached. */
    private static List<StepValueView> baseSteps(DebugFrame frame) {
        if (frame.getSource() instanceof Spreadsheet spreadsheet) {
            Set<String> executedRefs = executedRefs(frame);
            String currentRef = currentRef(frame);
            List<StepValueView> steps = new ArrayList<>();
            forEachCell(spreadsheet, cell -> {
                String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
                steps.add(StepValueView.builder()
                        .ref(ref)
                        .label(SpreadsheetCellNames.of(spreadsheet, cell))
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
        Set<String> covered = new HashSet<>();
        List<StepValueView> result = new ArrayList<>(steps.size());
        for (StepValueView step : steps) {
            covered.add(step.ref());
            List<CallNode> kids = children.get(step.ref());
            result.add(kids == null || kids.isEmpty()
                    ? step
                    : step.toBuilder().children(toCallNodeViews(kids)).build());
        }
        children.forEach((ref, kids) -> {
            if (!covered.contains(ref) && !kids.isEmpty()) {
                result.add(StepValueView.builder()
                        .ref(ref)
                        .status(StepStatus.EXECUTED)
                        .children(toCallNodeViews(kids))
                        .build());
            }
        });
        return result;
    }

    /** Convert returned sub-calls to views, recursively — structure only, never values. */
    private static List<CallNodeView> toCallNodeViews(List<CallNode> nodes) {
        return nodes.stream().map(TraceDebugMapper::toCallNodeView).toList();
    }

    private static CallNodeView toCallNodeView(CallNode node) {
        List<StepValueView> steps = node.steps().stream()
                .map(step -> StepValueView.builder()
                        .ref(step.ref())
                        .label(step.label())
                        .status(StepStatus.EXECUTED)
                        .durationMillis(toMillis(step.durationNanos()))
                        .selfMillis(selfMillis(step.durationNanos(), sumDurations(step.children().stream())))
                        .children(step.children().isEmpty() ? null : toCallNodeViews(step.children()))
                        .build())
                .toList();
        // Self time is the node's own work: its total minus the time spent in the tables it called.
        long childrenNanos = sumDurations(node.steps().stream().flatMap(step -> step.children().stream()));
        return CallNodeView.builder()
                .uri(node.uri())
                .name(node.name())
                .kind(node.kind())
                .durationMillis(toMillis(node.durationNanos()))
                .selfMillis(selfMillis(node.durationNanos(), childrenNanos))
                .steps(steps)
                .dispatch(node.dispatch())
                .refStep(node.refStep())
                .build();
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
        long childrenNanos = sumDurations(frame.getExecutedChildren().values().stream().flatMap(List::stream));
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
        Set<String> fired = Arrays.stream(firedRuleIndices)
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

    /** Apply an action to every real spreadsheet step cell, in grid order. */
    private static void forEachCell(Spreadsheet spreadsheet, Consumer<SpreadsheetCell> action) {
        for (SpreadsheetCell[] row : spreadsheet.getCells()) {
            for (SpreadsheetCell cell : row) {
                if (isStepCell(cell)) {
                    action.accept(cell);
                }
            }
        }
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
        CurrentLocation location = frame.getLocation();
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

    private @Nullable ParameterValue freezeResult(DebugFrame frame, Map<Object, Object> clones) {
        if (!frame.isCompleted() || frame.getResult() == null
                || !(frame.getSource() instanceof ExecutableRulesMethod method)) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("return", safeClone(frame.getResult(), clones), method.getType());
        return buildParameterValue(param, true);
    }

    private static Object safeClone(Object value, Map<Object, Object> clones) {
        if (value == null) {
            return null;
        }
        try {
            return Cloner.clone(value, clones);
        } catch (Exception e) {
            log.debug("Failed to freeze a value, using the live reference", e);
            return value;
        }
    }

    private List<MessageDescription> buildErrors(DebugFrame frame) {
        Throwable error = frame.getError();
        if (error == null) {
            return Collections.emptyList();
        }
        Throwable cause = Objects.requireNonNullElse(error.getCause(), error);
        List<MessageDescription> result = new ArrayList<>();
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
        List<DecisionConditionView> conditions = new ArrayList<>();
        for (ConditionCheck check : checks) {
            if (!(check.condition() instanceof IBaseCondition condition)) {
                continue;
            }
            String name = condition.getName();
            for (int rule : check.rules()) {
                conditions.add(new DecisionConditionView(name, decisionTable.getRuleName(rule), check.successful()));
            }
        }
        return new DecisionView(fired, conditions);
    }

    /** Build a parameter value, registering large values for lazy retrieval. */
    public ParameterValue buildParameterValue(ParameterWithValueDeclaration param, boolean preferLazy) {
        var type = param.getType();
        var rawValue = param.getValue();
        var description = type != null ? type.getDisplayName(INamedThing.SHORT) : null;
        var isSimple = type != null && type.isSimple();
        var builder = ParameterValue.builder()
                .name(param.getName())
                .description(description)
                .schema(generateSchema(type));
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
