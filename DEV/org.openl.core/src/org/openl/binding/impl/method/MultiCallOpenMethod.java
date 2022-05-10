package org.openl.binding.impl.method;

import java.lang.reflect.Array;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class MultiCallOpenMethod extends AOpenMethodDelegator {

    private final IMethodCaller methodCaller;
    private final Integer[] multiCallParameterIndexes;
    private final IOpenClass type;
    private final Class<?> componentType;

    public MultiCallOpenMethod(IMethodCaller methodCaller, boolean[] multiCallParameters) {
        super(methodCaller.getMethod());
        this.methodCaller = methodCaller;
        this.multiCallParameterIndexes = initMultiCallParameterIndexes(multiCallParameters);
        IOpenClass originalType = methodCaller.getMethod().getType();
        if (!JavaOpenClass.VOID.equals(originalType) && !JavaOpenClass.CLS_VOID.equals(originalType)) {
            this.type = methodCaller.getMethod().getType().getArrayType(1);
            this.componentType = methodCaller.getMethod().getType().getInstanceClass();
        } else {
            this.type = methodCaller.getMethod().getType();
            this.componentType = null;
        }
    }

    private Integer[] initMultiCallParameterIndexes(boolean[] multiCallParameters) {
        int c = 0;
        for (boolean x : multiCallParameters) {
            if (x) {
                c++;
            }
        }
        Integer[] res = new Integer[c];
        int i = 0;
        int j = 0;
        for (boolean x : multiCallParameters) {
            if (x) {
                res[j++] = i;
            }
            i++;
        }
        return res;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        int length = 1;
        for (Integer arrayArgArgument : multiCallParameterIndexes) {
            Object v = params[arrayArgArgument];
            if (v == null) {
                length = 0;
                break;
            }
            length *= Array.getLength(v);
        }

        Object[] callParameters = (Object[]) Array.newInstance(Object.class, params.length);
        System.arraycopy(params, 0, callParameters, 0, params.length);

        Object result = null;
        if (componentType != null) {
            result = Array.newInstance(componentType, length);
        }

        if (length > 0) {
            callDelegateAndPopulateResult(target, env, params, callParameters, 0, result, 0);
        }

        return result;
    }

    private int callDelegateAndPopulateResult(Object target,
            IRuntimeEnv env,
            Object[] params,
            Object[] callParameters,
            int iteratedArg,
            Object result,
            int callIndex) {
        int iteratedParamNum = multiCallParameterIndexes[iteratedArg];
        Object iteratedParameter = params[iteratedParamNum];
        int length = Array.getLength(iteratedParameter);
        for (int i = 0; i < length; i++) {
            callParameters[iteratedParamNum] = Array.get(iteratedParameter, i);
            if (iteratedArg < multiCallParameterIndexes.length - 1) {
                callIndex = callDelegateAndPopulateResult(target,
                    env,
                    params,
                    callParameters,
                    iteratedArg + 1,
                    result,
                    callIndex);
            } else {
                invokeMethodAndSetResultToArray(target, env, callParameters, result, callIndex);
                callIndex++;
            }
        }

        return callIndex;
    }

    @SuppressWarnings("unchecked")
    private void invokeMethodAndSetResultToArray(Object target,
            IRuntimeEnv env,
            Object[] callParameters,
            Object results,
            int index) {
        Object value;
        if (ArrayUtils.indexOf(callParameters, null) >= 0) {
            value = methodCaller.invoke(target, callParameters.clone(), env);
        } else {
            value = methodCaller.invoke(target, callParameters, env);
        }
        if (results != null) {
            Array.set(results, index, value);
        }
    }

    @Override
    public IOpenClass getType() {
        return type;
    }
}
