package org.openl.binding.impl;

import org.openl.types.IMethodCaller;
import org.openl.types.impl.MethodCallerDelegator;
import org.openl.vm.IRuntimeEnv;

public class BinaryMethodCallerSwapParams extends MethodCallerDelegator {

    public BinaryMethodCallerSwapParams(IMethodCaller delegate) {
        super(delegate);
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (params.length == 2) {

            Object[] params2 = new Object[]{params[1], params[0]};
            return super.invoke(target, params2, env);
        }

        return super.invoke(params[0], new Object[]{target}, env);
    }

}
