/*
 * Created on May 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodCaller implements IMethodCaller {

    private IOpenMethod method;

    public MethodCaller(IOpenMethod method) {
        this.method = method;
    }

    @Override
    public IOpenMethod getMethod() {
        return method;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodCaller#invoke(java.lang.Object[])
     */
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return method.invoke(target, params, env);
    }

}
