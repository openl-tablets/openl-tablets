package org.openl.studio.projects.service.trace;

import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Receives traced invocations on the worker thread.
 *
 * <p>{@link org.openl.studio.projects.service.trace.DebugTracer} holds the hook for a debug session and, once
 * attached to the run's environment, forwards every invocation to it, so the debugger sees the same call
 * chain the engine executes. The hook is responsible for actually running the invocation it brackets.
 */
public interface DebugHook {

    /**
     * Bracket one invocation: track it on the stack (or as a sub-step), possibly suspend, then run it.
     *
     * @return whatever the wrapped invocation returns
     */
    <T, E extends IRuntimeEnv, R> R bracketInvoke(Invokable<? super T, E> executor,
                                                  T target,
                                                  Object[] params,
                                                  E env,
                                                  Object source);

    /** Record a current-line change reported through {@code Tracer.put} and possibly suspend. */
    void onPut(Object source, String id, Object[] args);

    /**
     * Resolve a re-read of a step that may have already executed (for example a spreadsheet cell whose
     * value is cached). Returns {@code true} when the step is known and the re-read has been recorded as
     * a reference, so the engine does not report it again.
     */
    boolean onResolveNode(Object executor);
}
