package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.util.OpenClassUtils;

/**
 * Binder for methods that contains number of arguments in the signature of the
 * same type. So for the method call 'returnType foo(type1 param1, type2 param2,
 * type2 param3)' this binder will try to find the method 'returnType foo(type1
 * param1, type2[] params)'. Supports single argument in the signature.
 * 
 * @author DLiauchuk
 *
 */
public class VariableLengthArgumentsMethodBinder extends ANodeBinder {

    private String methodName;
    private IOpenClass[] argumentsTypes;
    private IBoundNode[] children;

    public VariableLengthArgumentsMethodBinder(String methodName, IOpenClass[] argumentsTypes, IBoundNode[] children) {
        this.methodName = methodName;
        if (argumentsTypes == null || argumentsTypes.length < 1) {
            String message = String.format(
                "At least one argument should exist in method signature(%s) " + "to bind it as variable arguments method",
                methodName);
            throw new OpenlNotCheckedException(message);
        }
        this.argumentsTypes = argumentsTypes.clone();
        if (children == null) {
            String message = String.format("Chldren nodes for method %s cannot be null", methodName);
            throw new OpenlNotCheckedException(message);
        }
        this.children = children.clone();
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode boundNode = findWithoutCasts(node, bindingContext);
        if (boundNode != null) { // If found without casts return it.
            return boundNode;
        }
        //Add casts logic here if needs.

        return null;
    }

    private IBoundNode findWithoutCasts(ISyntaxNode node, IBindingContext bindingContext) {
        for (int i = argumentsTypes.length - 1; i >= 0; i--) {
            IOpenClass[] args = new IOpenClass[i + 1];
            System.arraycopy(argumentsTypes, 0, args, 0, i);
            IOpenClass varArgType = argumentsTypes[i];
            for (int j = i + 1; j < argumentsTypes.length; j++) {
                varArgType = OpenClassUtils.findParentClass(varArgType, argumentsTypes[j]);
                if (varArgType == null) {
                    break;
                }
            }
            if (varArgType == null) {
                continue;
            }
            args[i] = varArgType.getAggregateInfo().getIndexedAggregateType(varArgType, 1);
            IMethodCaller matchedMethod = bindingContext
                .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, args);
            if (matchedMethod != null) {
                return new VariableArgumentsMethodBoundNode(node,
                    children,
                    matchedMethod,
                    i,
                    varArgType.getInstanceClass());
            }
        }
        return null;
    }
}
