/*
 * Created on May 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IMethodCaller {
    public IOpenMethod getMethod();

    public Object invoke(Object target, Object[] params, IRuntimeEnv env);
}
