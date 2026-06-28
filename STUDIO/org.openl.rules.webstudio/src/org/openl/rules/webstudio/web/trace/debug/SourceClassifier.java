package org.openl.rules.webstudio.web.trace.debug;

import org.jspecify.annotations.Nullable;

import org.openl.vm.IRuntimeEnv;

/**
 * Decides how a traced invocation maps onto the debugger model.
 *
 * <p>An invocation is either a table-level frame (a new stack frame), a sub-step inside the current
 * frame (a current-line change), or transparent (ignored). This seam keeps the engine independent of
 * concrete OpenL types so it can be tested with synthetic invocations.
 */
public interface SourceClassifier {

    /** Frame metadata for the given invocation source, or {@code null} when it is not a table frame. */
    @Nullable
    FrameDescriptor describeFrame(Object source);

    /**
     * Location for a sub-step executor (cell, fired rule, operation), or {@code null}.
     *
     * @param executor    the sub-step executor
     * @param env         the runtime environment
     * @param frameSource the owning table of the current frame, used to resolve display names
     */
    @Nullable
    CurrentLocation describeSubStep(Object executor, IRuntimeEnv env, @Nullable Object frameSource);

    /**
     * Decision-table condition result reported through {@code Tracer.put} ({@code index}/{@code
     * condition}), or {@code null} when the put is not a condition check.
     */
    @Nullable
    default ConditionCheck describeCondition(String id, Object[] args) {
        return null;
    }

    /**
     * Identity and display data of a table frame.
     *
     * @param kind frame kind
     * @param uri  source URI of the table, used for breakpoints and table rendering
     * @param name display name of the table
     */
    record FrameDescriptor(FrameKind kind, String uri, String name) {
    }
}
