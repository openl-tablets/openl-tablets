package org.openl.binding.impl.method;

import java.lang.reflect.Array;

import org.apache.commons.lang3.ArrayUtils;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.util.OpenClassUtils;
import org.openl.vm.IRuntimeEnv;

public class MultiCallOpenMethod extends AOpenMethodDelegator {

    protected IMethodCaller methodCaller;
    protected Integer[] multiCallParameterIndexes;
    protected IOpenClass type;
    protected Class<?> componentType;

    protected MultiCallOpenMethod(IMethodCaller methodCaller) {
        super(methodCaller.getMethod());
    }

    public MultiCallOpenMethod(IMethodCaller methodCaller, boolean[] multiCallParameters) {
        super(methodCaller.getMethod());
        this.methodCaller = methodCaller;
        this.multiCallParameterIndexes = initMultiCallParameterIndexes(multiCallParameters);
        IOpenClass originalType = methodCaller.getMethod().getType();
        if (!OpenClassUtils.isVoid(originalType)) {
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
        int resultLength = 1;
        for (Integer arrayArgArgument : multiCallParameterIndexes) {
            Object v = params[arrayArgArgument];
            if (v == null) {
                resultLength = 0;
                break;
            }
            resultLength *= Array.getLength(v);
        }

        Object[] callParameters = (Object[]) Array.newInstance(Object.class, params.length);
        System.arraycopy(params, 0, callParameters, 0, params.length);

        Object result = null;
        if (componentType != null) {
            result = Array.newInstance(componentType, resultLength);
        }

        if (resultLength > 0) {
            callDelegateAndPopulateResult(target, env, params, callParameters, 0, result, resultLength, 0);
        }

        return result;
    }

    private int callDelegateAndPopulateResult(Object target,
                                              IRuntimeEnv env,
                                              Object[] params,
                                              Object[] callParameters,
                                              int iteratedArg,
                                              Object result,
                                              int resultLength,
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
                        resultLength,
                        callIndex);
            } else {
                invokeMethodAndSetResultToArray(target, env, callParameters, result, resultLength, callIndex);
                callIndex++;
            }
        }

        return callIndex;
    }

    @SuppressWarnings("unchecked")
    protected void invokeMethodAndSetResultToArray(Object target,
                                                   IRuntimeEnv env,
                                                   Object[] callParameters,
                                                   Object results,
                                                   int resultLength,
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
