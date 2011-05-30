package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public class VariableArgumentsMethodBoundNode extends MethodBoundNode {
    
    private int indexOfFirstVarArg;
    private Class<?> componentVarArgClass;

    public VariableArgumentsMethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, 
            IMethodCaller methodWithLastArrayArgument, int indexOfLastEqualArgumentType, Class<?> componentVarArgClass) {
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
        List<Object> parametersList = new ArrayList<Object>();
        if (indexOfFirstVarArg > 0) {
            // first parameters should be copied as is.
            //
            for (int i = 0; i < indexOfFirstVarArg; i++) {
                parametersList.add(methodParameters[i]);
            }
        }
        // all the parameters of the same type in the tail of parameters sequence, 
        // should be wrapped by array of this type
        //
        Object sameTypeParameters = getAllParametersOfTheSameType(methodParameters);
        
        parametersList.add(sameTypeParameters);        
        
        return (Object[]) parametersList.toArray(new Object[parametersList.size()]);

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
