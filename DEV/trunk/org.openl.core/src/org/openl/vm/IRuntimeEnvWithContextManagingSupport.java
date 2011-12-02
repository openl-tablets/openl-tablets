package org.openl.vm;

import org.openl.runtime.IRuntimeContext;

/**
 * Runtime environment that supports runtime context managing.
 * 
 * It is provided by storing runtime context in stack.
 * 
 * @author PUdalau
 */
public interface IRuntimeEnvWithContextManagingSupport extends IRuntimeEnv {

    IRuntimeContext popContext();

    void pushContext(IRuntimeContext context);
}
