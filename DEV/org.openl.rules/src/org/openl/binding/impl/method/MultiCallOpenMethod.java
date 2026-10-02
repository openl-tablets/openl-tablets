package org.openl.binding.impl.method;

import java.lang.reflect.Array;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.OpenClassUtils;
import org.openl.vm.IRuntimeEnv;

public class MultiCallOpenMethod extends AOpenMethodDelegator {

    protected IMethodCaller methodCaller;
    protected Integer[] multiCallParameterIndexes;
    @Getter
    protected IOpenClass type;
    protected Class<?> componentType;

    protected MultiCallOpenMethod(IMethodCaller methodCaller) {
        super(methodCaller.getMethod());
    }

    public MultiCallOpenMethod(IMethodCaller methodCaller, boolean[] multiCallParameters) {
        super(methodCaller.getMethod());
        this.methodCaller = methodCaller;
        this.multiCallParameterIndexes = initMultiCallParameterIndexes(multiCallParameters);
        var originalType = methodCaller.getMethod().getType();
        if (!OpenClassUtils.isVoid(originalType)) {
            this.type = methodCaller.getMethod().getType().getArrayType(1);
            this.componentType = methodCaller.getMethod().getType().getInstanceClass();
        } else {
            this.type = methodCaller.getMethod().getType();
            this.componentType = null;
        }
    }

    private Integer[] initMultiCallParameterIndexes(boolean[] multiCallParameters) {
        var c = 0;
        for (boolean x : multiCallParameters) {
            if (x) {
                c++;
            }
        }
        Integer[] res = new Integer[c];
        var i = 0;
        var j = 0;
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
        var resultLength = 1;
        for (Integer arrayArgArgument : multiCallParameterIndexes) {
            var v = params[arrayArgArgument];
            if (v == null) {
                resultLength = 0;
                break;
            }
            resultLength *= Array.getLength(v);
        }

        var callParameters = (Object[]) Array.newInstance(Object.class, params.length);
        System.arraycopy(params, 0, callParameters, 0, params.length);

        Object result = null;
        if (componentType != null) {
            result = Array.newInstance(componentType, resultLength);
        }

        for (var callIndex = 0; callIndex < resultLength; callIndex++) {
            putCallElements(params, callParameters, callIndex);
            invokeMethodAndSetResultToArray(target, env, callParameters, result, resultLength, callIndex);
        }

        return result;
    }

    /**
     * Puts the array elements of the call with the given index into the call parameters.
     *
     * <p>The calls run through every combination of the array elements. The last array argument changes the fastest.
     */
    private void putCallElements(Object[] params, Object[] callParameters, int callIndex) {
        var rest = callIndex;
        for (var i = multiCallParameterIndexes.length - 1; i >= 0; i--) {
            var paramNum = multiCallParameterIndexes[i];
            var array = params[paramNum];
            var length = Array.getLength(array);
            callParameters[paramNum] = Array.get(array, rest % length);
            rest /= length;
        }
    }

    // The multithreaded subclass overrides it and reads the result length to decide whether to call in parallel.
    @SuppressWarnings({"unchecked", "java:S1172"})
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

    public IOpenMethod getSourceMethod() {
        return extractMethod(this);
    }

    private IOpenMethod extractMethod(IOpenMethod openMethod) {
        if (openMethod instanceof AOpenMethodDelegator delegator) {
            return extractMethod(delegator.getDelegate());
        }
        return openMethod;
    }
}
