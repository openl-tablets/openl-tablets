/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.vm;

import org.openl.IOpenRunner;
import org.openl.runtime.IRuntimeContext;

/**
 * @author snshor
 */
public interface IRuntimeEnv {
    Object[] getLocalFrame();

    IOpenRunner getRunner();

    Object getThis();

    Object[] popLocalFrame();

    Object popThis();

    void pushLocalFrame(Object[] frame);

    void pushThis(Object thisObject);

    /**
     * Gets the runtime context.
     *
     * @return <code>IContext</code> instance
     */
    IRuntimeContext getContext();

    /**
     * Sets context to runtime environment. By default, runtime environment does not provide any context.
     *
     * @param context runtime context.
     */
    void setContext(IRuntimeContext context);

    boolean isContextManagingSupported();

    IRuntimeContext popContext();

    void pushContext(IRuntimeContext context);

    IRuntimeEnv copy();

    /**
     * The tracer observing this execution — the no-op {@link Tracer#NONE} unless a tracing caller attached one.
     *
     * <p>Never {@code null}: callers invoke it directly (for example {@code getTracer().invoke(...)}) without a
     * null check, and ordinary execution passes straight through the no-op tracer.
     */
    default Tracer getTracer() {
        return Tracer.NONE;
    }

    /** Attach the tracer that observes this execution; {@code null} clears it back to the no-op tracer. */
    default void setTracer(Tracer tracer) {
        // Environments that do not support tracing ignore it and stay untraced.
    }
}
