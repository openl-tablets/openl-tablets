package org.openl.rules.webstudio.web.trace.debug;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A node of the executed call tree: a table invocation that has already returned, kept as structure only.
 *
 * <p>Carries the table's identity and the sub-steps that ran, with the sub-calls each of them made. No
 * parameters or results are retained, so the executed tree stays cheap — bounded by structure, not by the
 * values that flowed through it.
 *
 * @param uri           source URI of the table
 * @param name          display name of the table
 * @param kind          kind of the table
 * @param durationNanos real execution time of this invocation, excluding time spent parked at suspend points
 * @param steps         the sub-steps that executed, each with the sub-calls it made
 * @param dispatch      set when this table was selected by a dispatcher (overloaded by dimensions), else {@code null}
 * @param refStep       for a {@link FrameKind#STEP_REF} node, the reference of the step it points at, else {@code null}
 */
public record CallNode(String uri, String name, FrameKind kind, long durationNanos, List<Step> steps,
                       @Nullable DispatchInfo dispatch, @Nullable String refStep) {

    /**
     * One executed sub-step (a spreadsheet cell or a decision-table rule) and the sub-calls it made.
     *
     * @param ref           short reference of the step (for example {@code R2C3})
     * @param label         human-readable name, or {@code null}
     * @param durationNanos real execution time of the step (its own work plus the tables it called)
     * @param children      the table invocations this step made, in execution order
     */
    public record Step(String ref, @Nullable String label, long durationNanos, List<CallNode> children) {
    }

    /**
     * A reference to a step that already executed elsewhere in the same frame: a formula computed or
     * re-read another cell. Carries no time of its own — the execution is accounted at the referenced step.
     */
    static CallNode referenceTo(String uri, String ref, @Nullable String label) {
        return new CallNode(uri, label != null ? label : ref, FrameKind.STEP_REF, 0, List.of(), null, ref);
    }
}
