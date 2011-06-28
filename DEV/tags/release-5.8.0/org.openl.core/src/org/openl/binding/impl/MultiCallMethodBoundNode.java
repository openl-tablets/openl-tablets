package org.openl.binding.impl;

import java.lang.reflect.Array;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Bound node for methods such as <code>'double[] calculate(Premium[] premiumObj)'</code>. Is based on the method with 
 * signature <code>'double calculate(Premium premiumObj)'</code> by evaluating it several times on runtime.
 * 
 * @author DLiauchuk
 *
 */
public class MultiCallMethodBoundNode extends MethodBoundNode {
    
    private IOpenClass returnType;
    
    /** the index of the argument in the method signature that is an array**/
    private int arrayArgumentIndex;
    
    /**
     * 
     * @param syntaxNode will be represents like <code>'calculate(parameter)'</code>
     * @param children its gonna be only one children, that represents the parameter in method call.
     * @param singleParameterMethod method for single(not array) parameter in signature
     * @param arrayArgumentIndex the index of the argument in method signature that is an array
     */
    public MultiCallMethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, IMethodCaller singleParameterMethod, int arrayArgumentIndex) {
        super(syntaxNode, children, singleParameterMethod);
        this.arrayArgumentIndex = arrayArgumentIndex;
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
        Object[] methodParameters = evaluateChildren(env);
        
        // gets the values of array parameters
        Object arrayParameters = methodParameters[arrayArgumentIndex];
        int paramsLenght = Array.getLength(arrayParameters);        
        
        // create an array of results        
        Object results = Array.newInstance(super.getType().getInstanceClass(), paramsLenght);
        
        // populate the results array by invoking method for single parameter
        for (int callIndex = 0; callIndex < paramsLenght; callIndex++) {
            Array.set(results , callIndex, getMethodCaller().invoke(target, 
                initParametersForSingleCall(methodParameters, arrayParameters, callIndex), env));            
        }
        return results;
    }
    
    private Object[] initParametersForSingleCall(Object[] allParameters, Object arrayParameters, int callIndex) {
        // create an array of parameters that will be used for current call
        //
        Object[] callParameters = (Object[]) Array.newInstance(Object.class, allParameters.length);
        
        // populate call parameters with values from original method parameters
        //
        for (int i = 0; i < allParameters.length; i++) {            
            if (i == arrayArgumentIndex) {
                // for this call number use the appropriate value from the array parameter
                //
                Array.set(callParameters, i, Array.get(arrayParameters, callIndex));
            } else {
                // use the original parameter.
                //
                Array.set(callParameters, i, allParameters[i]);
            }
        }
        return callParameters;
    }

    public IOpenClass getType() {
        if (returnType == null) {
            // gets the return type of bound node, it will be the single type.
            //
            IOpenClass singleReturnType = super.getType();
            
            // create an array type.
            //
            returnType = singleReturnType.getAggregateInfo().getIndexedAggregateType(singleReturnType, 1);
        }        
        return returnType;
    }
}
