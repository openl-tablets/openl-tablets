package org.openl.rules.webstudio.web.trace.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.runtime.IRuntimeContext;

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
    private final List<ConditionCheck> conditionChecks = new ArrayList<>();
    /** Returned sub-calls grouped by the step that made them; populated only in profiling mode. */
    private final Map<String, List<CallNode>> executedChildren = new LinkedHashMap<>();
    private int executedChildCount;

    private @Nullable CurrentLocation location;
    private @Nullable Object currentStep;
    private @Nullable Object result;
    private @Nullable Throwable error;
    private boolean completed;
    /** Real execution time of this frame, excluding time parked at suspend points; set when the frame returns. */
    private long durationNanos;
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

    void setLocation(CurrentLocation location) {
        this.location = location;
    }

    void setCurrentStep(@Nullable Object currentStep) {
        this.currentStep = currentStep;
    }

    void recordExecutedStep(String ref, @Nullable String label, @Nullable Object value, long durationNanos) {
        if (executedSteps.size() < MAX_RECORDED_PER_FRAME) {
            executedSteps.add(new ExecutedStep(ref, label, value, durationNanos));
        }
    }

    void recordConditionCheck(ConditionCheck check) {
        if (conditionChecks.size() < MAX_RECORDED_PER_FRAME) {
            conditionChecks.add(check);
        }
    }

    /** Record a returned sub-call's structure under the step that made it (profiling mode only). */
    void recordExecutedChild(@Nullable String callerRef, CallNode child) {
        if (executedChildCount >= MAX_RECORDED_PER_FRAME) {
            return;
        }
        executedChildren.computeIfAbsent(callerRef == null ? "" : callerRef, key -> new ArrayList<>()).add(child);
        executedChildCount++;
    }

    /** Snapshot this frame as an executed call-tree node: its sub-steps and their sub-calls, no values. */
    CallNode toCallNode() {
        List<CallNode.Step> steps = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (ExecutedStep step : executedSteps) {
            if (covered.add(step.ref())) {
                steps.add(new CallNode.Step(step.ref(), step.label(), step.durationNanos(),
                        List.copyOf(childrenOf(step.ref()))));
            }
        }
        executedChildren.forEach((ref, children) -> {
            if (!covered.contains(ref)) {
                steps.add(new CallNode.Step(ref, null, 0, List.copyOf(children)));
            }
        });
        return new CallNode(uri, name, kind, durationNanos, steps, dispatch);
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

    void setDispatch(DispatchInfo dispatch) {
        this.dispatch = dispatch;
    }
}
