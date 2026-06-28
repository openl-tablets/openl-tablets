package org.openl.rules.webstudio.web.trace.debug;

import java.util.ArrayList;
import java.util.List;

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
     * @param ref   short reference of the step (for example {@code R2C3} for a spreadsheet cell)
     * @param label human-readable label, or {@code null}
     * @param value the computed value (a live reference, frozen on inspection)
     */
    public record ExecutedStep(String ref, @Nullable String label, @Nullable Object value) {
    }

    /** Upper bound on recorded sub-steps, so a long loop or huge spreadsheet cannot grow unbounded. */
    private static final int MAX_EXECUTED_STEPS = 5000;

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

    private @Nullable CurrentLocation location;
    private @Nullable Object currentStep;
    private @Nullable Object result;
    private @Nullable Throwable error;
    private boolean completed;

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

    void recordExecutedStep(String ref, @Nullable String label, @Nullable Object value) {
        if (executedSteps.size() < MAX_EXECUTED_STEPS) {
            executedSteps.add(new ExecutedStep(ref, label, value));
        }
    }

    void recordConditionCheck(ConditionCheck check) {
        conditionChecks.add(check);
    }

    void completeWith(@Nullable Object result) {
        this.result = result;
        this.completed = true;
    }

    void failWith(Throwable error) {
        this.error = error;
        this.completed = true;
    }
}
