package org.openl.binding.impl;

import java.lang.reflect.Array;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public class VariableArgumentsMethodBoundNode extends MethodBoundNode {
    
    private int indexOfFirstVarArg;
    private Class<?> componentVarArgClass;

    public VariableArgumentsMethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, 
            IMethodCaller methodWithLastArrayArgument, int indexOfLastEqualArgumentType, 
            Class<?> componentVarArgClass) {
        super(syntaxNode, children, methodWithLastArrayArgument);
        this.indexOfFirstVarArg = indexOfLastEqualArgumentType;
        this.componentVarArgClass = componentVarArgClass;
    }
    
    @Override
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
        Object[] methodParameters = evaluateChildren(env);
        return getMethodCaller().invoke(target, modifyParameters(methodParameters), env);        
    }

    private Object[] modifyParameters(Object[] methodParameters) {
        int parametersCount = getMethodCaller().getMethod().getSignature().getNumberOfParameters();
        Object[] modifiedParameters = new Object[parametersCount];
        System.arraycopy(methodParameters, 0, modifiedParameters, 0, indexOfFirstVarArg);

        // all the parameters of the same type in the tail of parameters sequence, 
        // should be wrapped by array of this type
        //
        modifiedParameters[parametersCount - 1] = getAllParametersOfTheSameType(methodParameters);
        return modifiedParameters;
    }

    private Object getAllParametersOfTheSameType(Object[] methodParameters) {        
        int parametersOfTheSameType = methodParameters.length - indexOfFirstVarArg;
        Object sameTypeParameters = Array.newInstance(componentVarArgClass, parametersOfTheSameType);
        
        for (int i = 0; i < parametersOfTheSameType; i++) {
            Array.set(sameTypeParameters, i, methodParameters[i + indexOfFirstVarArg]);
        }
        return sameTypeParameters;
    }

    
}
