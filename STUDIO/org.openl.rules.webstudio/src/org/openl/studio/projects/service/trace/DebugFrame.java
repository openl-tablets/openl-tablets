package org.openl.studio.projects.service.trace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.dt.IDecisionTable;
import org.openl.runtime.IRuntimeContext;
import org.openl.studio.projects.model.trace.DecisionRow;
import org.openl.studio.projects.model.trace.DispatchInfo;
import org.openl.studio.projects.model.trace.FrameKind;

/**
 * One table invocation on the live execution stack.
 *
 * <p>A frame holds only live references to the running arguments, target, and context; nothing is
 * cloned when the frame is entered. Parameters are frozen lazily, on demand, while execution is
 * suspended. The frame is dropped from the stack as soon as its table returns, so memory stays bounded
 * by the live stack depth.
 *
 * <p>As sub-steps (spreadsheet cells, fired decision-table rules) execute, the frame records each one
 * with its computed value, so an analyst can inspect already-executed steps while a later step runs.
 * The {@link #currentStep} is the live sub-step source used to highlight the current line in the table.
 */
@Getter
public final class DebugFrame {

    /**
     * A sub-step that has finished inside this frame, with its computed value.
     *
     * @param ref           short reference of the step (for example {@code R2C3} for a spreadsheet cell)
     * @param label         human-readable label, or {@code null}
     * @param value         the computed value (a live reference, frozen on inspection)
     * @param durationNanos real execution time of the step (its own work plus the tables it called), minus parked time
     */
    public record ExecutedStep(String ref, @Nullable String label, @Nullable Object value, long durationNanos) {
    }

    /** Upper bound on recorded sub-steps and condition checks, so a long loop or huge table cannot grow unbounded. */
    private static final int MAX_RECORDED_PER_FRAME = 5000;

    private final FrameKind kind;
    private final Object source;
    private final String uri;
    private final String name;
    private final @Nullable Object target;
    private final Object[] params;
    private final @Nullable IRuntimeContext context;
    private final int depth;
    private final List<ExecutedStep> executedSteps = new ArrayList<>();
    /** Executed steps by their live executor, so a later re-read of the same cell can reference them. */
    @Getter(lombok.AccessLevel.NONE)
    private final Map<Object, ExecutedStep> executedByExecutor = new IdentityHashMap<>();
    private final List<ConditionCheck> conditionChecks = new ArrayList<>();
    /** Returned sub-calls grouped by the step that made them; populated only in profiling mode. */
    private final Map<String, List<CallNode>> executedChildren = new LinkedHashMap<>();

    private @Nullable CurrentLocation location;
    private @Nullable Object currentStep;
    private @Nullable Object result;
    private @Nullable Throwable error;
    private boolean completed;
    /** Zero-based execution number of this table (its 1st, 2nd, ... invocation in the run), for watch series. */
    private int invocationIndex;
    /** Real execution time of this frame, excluding time parked at suspend points; set when the frame returns. */
    private long durationNanos;
    /** Sum of the durations of this frame's direct sub-call frames, so its own time is {@code duration - childNanos}. */
    private long childNanos;
    /** Remaining nodes this frame's subtree may still retain, so one big branch cannot starve its siblings. */
    private long budget;
    /** Direct sub-calls this frame made that ran but were dropped once the tree hit its size limit. */
    private long notRetained;
    /** Set when this frame's table was selected by a dispatcher (a group of versions overloaded by dimensions). */
    private @Nullable DispatchInfo dispatch;

    public DebugFrame(SourceClassifier.FrameDescriptor descriptor,
                      Object source,
                      @Nullable Object target,
                      Object @Nullable [] params,
                      @Nullable IRuntimeContext context,
                      int depth) {
        this.kind = descriptor.kind();
        this.uri = descriptor.uri();
        this.name = descriptor.name();
        this.source = source;
        this.target = target;
        this.params = params == null ? new Object[0] : params;
        this.context = context;
        this.depth = depth;
    }

    void setLocation(@Nullable CurrentLocation location) {
        this.location = location;
    }

    void setInvocationIndex(int invocationIndex) {
        this.invocationIndex = invocationIndex;
    }

    void setCurrentStep(@Nullable Object currentStep) {
        this.currentStep = currentStep;
    }

    void recordExecutedStep(Object executor, String ref, @Nullable String label, @Nullable Object value,
                            long durationNanos) {
        if (executedSteps.size() < MAX_RECORDED_PER_FRAME) {
            var step = new ExecutedStep(ref, label, value, durationNanos);
            executedSteps.add(step);
            executedByExecutor.putIfAbsent(executor, step);
        }
    }

    /** The already-executed step run by the given executor, or {@code null} if it has not run in this frame. */
    @Nullable
    ExecutedStep executedStepFor(Object executor) {
        return executedByExecutor.get(executor);
    }

    void recordConditionCheck(ConditionCheck check) {
        if (conditionChecks.size() < MAX_RECORDED_PER_FRAME) {
            conditionChecks.add(check);
        }
    }

    /**
     * Record a returned sub-call's structure under the step that made it (profiling mode only). Retention is
     * bounded solely by the node cap in {@code admitNode}, which the caller checks before recording; there is
     * no separate per-frame breadth cap here, so an admitted sub-call is never silently dropped afterwards.
     */
    void recordExecutedChild(@Nullable String callerRef, CallNode child) {
        executedChildren.computeIfAbsent(callerRef == null ? "" : callerRef, key -> new ArrayList<>()).add(child);
    }

    /**
     * Snapshot this frame as an executed call-tree node: its sub-steps and their sub-calls, no values.
     *
     * <p>The table URI, name, and each step reference pass through {@code intern}, so the same table
     * repeated across thousands of nodes shares one instance of each string instead of duplicating it.
     */
    CallNode toCallNode(UnaryOperator<String> intern) {
        // A decision table breaks down into its evaluated conditions (matched/unmatched, like the legacy
        // detailed trace) plus the rule it returned, instead of a bare fired-rule step.
        if (source instanceof IDecisionTable decisionTable) {
            return new CallNode(intern.apply(uri), intern.apply(name), invocationIndex, kind, durationNanos,
                    decisionSteps(decisionTable, intern), dispatch, null, notRetained, childNanos);
        }
        var steps = new ArrayList<CallNode.Step>();
        var covered = new HashSet<String>();
        for (ExecutedStep step : executedSteps) {
            if (covered.add(step.ref())) {
                steps.add(new CallNode.Step(intern.apply(step.ref()),
                        step.label() == null ? null : intern.apply(step.label()),
                        step.durationNanos(), List.copyOf(childrenOf(step.ref()))));
            }
        }
        executedChildren.forEach((ref, children) -> {
            if (!covered.contains(ref)) {
                steps.add(new CallNode.Step(intern.apply(ref), null, 0, List.copyOf(children)));
            }
        });
        // A spreadsheet's plain value and constant cells never execute (cells evaluate lazily and these
        // are read, not run), yet they are table content — list them so the tree shows the whole table,
        // in grid order so a static line sits where the table puts it.
        if (source instanceof Spreadsheet spreadsheet) {
            for (SpreadsheetCell[] row : spreadsheet.getCells()) {
                for (SpreadsheetCell cell : row) {
                    if (cell == null) {
                        continue;
                    }
                    SpreadsheetCellType type = cell.getSpreadsheetCellType();
                    String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
                    if ((type == SpreadsheetCellType.VALUE || type == SpreadsheetCellType.CONSTANT)
                            && !covered.contains(ref)) {
                        steps.add(new CallNode.Step(intern.apply(ref),
                                intern.apply(SpreadsheetCellNames.of(spreadsheet, cell)), 0, List.of(), true));
                    }
                }
            }
            steps.sort(Comparator.comparingLong(DebugFrame::gridOrder));
        }
        return new CallNode(intern.apply(uri), intern.apply(name), invocationIndex, kind, durationNanos,
                steps, dispatch, null, notRetained, childNanos);
    }

    /**
     * The sub-steps of a decision-table node: one row per evaluated condition (in evaluation order, marked
     * matched or not), then the rule the table returned. Reproduces the legacy detailed-trace breakdown.
     */
    private List<CallNode.Step> decisionSteps(IDecisionTable decisionTable, UnaryOperator<String> intern) {
        List<CallNode.Step> steps = new ArrayList<>();
        int index = 0;
        for (ConditionCheck check : new LinkedHashSet<>(conditionChecks)) {
            String label = "Condition: " + check.conditionName() + ", Rules: " + ruleNames(decisionTable, check.rules());
            steps.add(new CallNode.Step(intern.apply("c" + index++), intern.apply(label), 0, List.of(), false,
                    check.successful() ? DecisionRow.MATCHED : DecisionRow.UNMATCHED));
        }
        // The returned rule is the fired-rule step; relabel it and let it keep the sub-calls its action made.
        Set<String> covered = new HashSet<>();
        for (ExecutedStep step : executedSteps) {
            if (covered.add(step.ref())) {
                String rules = step.label() == null ? step.ref() : step.label();
                steps.add(new CallNode.Step(intern.apply(step.ref()), intern.apply("Returned rule: [" + rules + "]"),
                        step.durationNanos(), List.copyOf(childrenOf(step.ref())), false, DecisionRow.RETURNED));
            }
        }
        return steps;
    }

    /**
     * Rule names of the given indices as {@code [R1, R2]}, matching the legacy condition-node labels. An
     * index node can list the same rule several times; the names are distinct so the label stays readable.
     */
    private static String ruleNames(IDecisionTable decisionTable, int[] rules) {
        return Arrays.stream(rules).distinct().mapToObj(decisionTable::getRuleName)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /** Grid position of a spreadsheet step by its {@code RnCm} reference, for a table-shaped ordering. */
    private static long gridOrder(CallNode.Step step) {
        String ref = step.ref();
        int c = ref.indexOf('C');
        if (ref.isEmpty() || ref.charAt(0) != 'R' || c < 0) {
            return Long.MAX_VALUE;
        }
        try {
            return Long.parseLong(ref.substring(1, c)) * 10_000 + Long.parseLong(ref.substring(c + 1));
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private List<CallNode> childrenOf(String ref) {
        return executedChildren.getOrDefault(ref, List.of());
    }

    void completeWith(@Nullable Object result) {
        this.result = result;
        this.completed = true;
    }

    void failWith(Throwable error) {
        this.error = error;
        this.completed = true;
    }

    void setDurationNanos(long durationNanos) {
        this.durationNanos = durationNanos;
    }

    /** Add a completed direct sub-call's time, so this frame's own time excludes the tables it called. */
    void addChildNanos(long nanos) {
        this.childNanos += nanos;
    }

    /** Set the number of nodes this frame's subtree may still retain (bounded by its parent and the branch cap). */
    void setBudget(long budget) {
        this.budget = budget;
    }

    /** Consume one node from this frame's subtree budget as a descendant is retained under it. */
    void decrementBudget() {
        this.budget--;
    }

    /** Record that one direct sub-call of this frame ran but was dropped from the tree once it hit its limit. */
    void incrementNotRetained() {
        this.notRetained++;
    }

    void setDispatch(DispatchInfo dispatch) {
        this.dispatch = dispatch;
    }
}
