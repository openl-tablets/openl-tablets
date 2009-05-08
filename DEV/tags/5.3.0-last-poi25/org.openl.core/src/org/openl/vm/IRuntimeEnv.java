/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.vm;

import org.openl.IOpenRunner;

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

}
