package org.openl.binding.impl.method;

import org.openl.rules.core.ce.ServiceMT;
import org.openl.vm.IRuntimeEnv;

public class MultiCallOpenMethodMT extends MultiCallOpenMethod {

    public MultiCallOpenMethodMT(MultiCallOpenMethod multiCallOpenMethod) {
        super(multiCallOpenMethod.getMethod());
        this.methodCaller = multiCallOpenMethod.methodCaller;
        this.type = multiCallOpenMethod.type;
        this.multiCallParameterIndexes = multiCallOpenMethod.multiCallParameterIndexes;
        this.componentType = multiCallOpenMethod.componentType;
    }

    @Override
    protected void invokeMethodAndSetResultToArray(Object target,
                                                   IRuntimeEnv env,
                                                   Object[] callParameters,
                                                   Object results,
                                                   int resultLength,
                                                   int index) {
        if (resultLength <= 1) {
            super.invokeMethodAndSetResultToArray(target, env, callParameters, results, resultLength, index);
        } else {
            final Object[] callParameters1 = callParameters.clone();
            ServiceMT.getInstance().execute(env, e -> {
                super.invokeMethodAndSetResultToArray(target, e, callParameters1, results, resultLength, index);
            });
        }
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object result = super.invoke(target, params, env);
        ServiceMT.getInstance().join(env);
        return result;
    }
}
