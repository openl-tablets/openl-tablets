/**
 * Created Jul 21, 2007
 */
package org.openl.types.impl;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ParameterMethodCaller implements IMethodCaller {
    int paramN;
    IOpenMethod method;

    public ParameterMethodCaller(IOpenMethod method, int paramn) {
        this.method = method;
        paramN = paramn;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return params[paramN];
    }
}
