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
    
    /**
     * 
     * @param syntaxNode will be represents like <code>'calculate(parameter)'</code>
     * @param children its gonna be only one children, that represents the parameter in method call.
     * @param singleParameterMethod method for single(not array) parameter in signature
     */
    public MultiCallMethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, IMethodCaller singleParameterMethod) {
        super(syntaxNode, children, singleParameterMethod);
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
        Object[] pars = evaluateChildren(env);
        
        // consider that params will have only one element. And it is an array
        Object functionParam = pars[0];
        int paramsLenght = Array.getLength(functionParam);        
        
        // create an array of results        
        Object results = Array.newInstance(super.getType().getInstanceClass(), paramsLenght);
        
        // populate the results array by invoking method for single parameter
        for (int i = 0; i < paramsLenght; i++) {
            Array.set(results , i, getMethodCaller().invoke(target, new Object[]{Array.get(functionParam, i)}, env));            
        }
        return results;
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
