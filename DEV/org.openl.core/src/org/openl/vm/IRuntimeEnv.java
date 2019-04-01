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
 *
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
     * Sets context to runtime environment. By default, runtime environment doesn't provide any context.
     *
     * @param context runtime context.
     */
    void setContext(IRuntimeContext context);

    boolean isContextManagingSupported();

    IRuntimeContext popContext();

    void pushContext(IRuntimeContext context);
}
