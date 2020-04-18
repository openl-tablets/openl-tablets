package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Bound node for methods such as <code>'double[] calculate(Premium[] premiumObj)'</code>. Is based on the method with
 * signature <code>'double calculate(Premium premiumObj)'</code> by evaluating it several times on runtime.
 *
 * @author DLiauchuk
 */
public class MultiCallMethodBoundNode extends MethodBoundNode {

    /**
     * cached return type for current bound node
     */
    private IOpenClass returnType;
    private Class<?> arrayClass;

    /**
     * the indexes of the arguments in the method signature that are arrays
     **/
    private final int[] arrayArgArguments;

    /**
     * @param syntaxNode will be represents like <code>'calculate(parameter)'</code>
     * @param children its gonna be only one children, that represents the parameter in method call.
     * @param singleParameterMethod method for single(not array) parameter in signature
     * @param arrayArgArgumentList the indexes of the arguments in the method signature that is are arrays
     */
    public MultiCallMethodBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] children,
            IMethodCaller singleParameterMethod,
            List<Integer> arrayArgArgumentList) {
        super(syntaxNode, singleParameterMethod, children);
        returnType = singleParameterMethod.getMethod().getType();

        if (!JavaOpenClass.VOID.equals(returnType)) {
            arrayClass = returnType.getInstanceClass();
            // create an array type.
            returnType = returnType.getArrayType(1);
        }

        this.arrayArgArguments = new int[arrayArgArgumentList.size()];

        for (int i = 0; i < arrayArgArgumentList.size(); i++) {
            arrayArgArguments[i] = arrayArgArgumentList.get(i);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object target = getTarget(env);
        Object[] methodParameters = evaluateChildren(env);

        // gets the values of array parameters

        int paramsLength = 1;
        for (Integer arrayArgArgument : arrayArgArguments) {
            Object arrayParameters = methodParameters[arrayArgArgument];
            if (arrayParameters == null) {
                paramsLength = 0;
                break;
            }
            paramsLength *= Array.getLength(arrayParameters);
        }

        Object results = null;

        Object[] callParameters = (Object[]) Array.newInstance(Object.class, methodParameters.length);
        System.arraycopy(methodParameters, 0, callParameters, 0, methodParameters.length);

        IMethodCaller methodCaller = getMethodCaller(env);

        if (arrayClass != null) {
            // create an array of results
            //
            results = Array.newInstance(arrayClass, paramsLength);
        }

        if (paramsLength > 0) {
            // populate the results array by invoking method for single parameter
            call(methodCaller, target, env, methodParameters, callParameters, 0, results, 0, paramsLength);
        }

        return results;
    }

    private int call(IMethodCaller methodCaller,
            Object target,
            IRuntimeEnv env,
            Object[] allParameters,
            Object[] callParameters,
            int iteratedArg,
            Object results,
            int callIndex,
            int resultLength) {
        int iteratedParamNum = arrayArgArguments[iteratedArg];
        Object iteratedParameter = allParameters[iteratedParamNum];
        int length = Array.getLength(iteratedParameter);
        for (int i = 0; i < length; i++) {
            callParameters[iteratedParamNum] = Array.get(iteratedParameter, i);

            if (iteratedArg < arrayArgArguments.length - 1) {
                callIndex = call(methodCaller,
                    target,
                    env,
                    allParameters,
                    callParameters,
                    iteratedArg + 1,
                    results,
                    callIndex,
                    resultLength);
            } else {
                invokeMethodAndSetResultToArray(methodCaller,
                    target,
                    env,
                    callParameters,
                    results,
                    callIndex,
                    resultLength);
                callIndex++;
            }
        }

        return callIndex;
    }

    protected IMethodCaller getMethodCaller(IRuntimeEnv env) { // Optimize if possible
        return getMethodCaller();
    }

    @SuppressWarnings("unchecked")
    protected void invokeMethodAndSetResultToArray(IMethodCaller methodCaller,
            Object target,
            IRuntimeEnv env,
            Object[] callParameters,
            Object results,
            int index,
            int resultLength) {
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
        return returnType;
    }

}
