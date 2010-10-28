/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class BinaryOperatorAndNodeBinder extends BinaryOperatorNodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 2) {

            BindHelper.processError("Binary node must have 2 subnodes", node, bindingContext);

            return new ErrorBoundNode(node);
            //            throw new BoundError("Binary node must have 2 subnodes", null, node);
        }

        IBoundNode[] children = bindChildren(node, bindingContext);
        IOpenClass[] types = getTypes(children);

        if ((types[0].getInstanceClass() == boolean.class || types[0].getInstanceClass() == Boolean.class)
            && (types[1].getInstanceClass() == boolean.class || types[1].getInstanceClass() == Boolean.class)) {

            return new BinaryOpNodeAnd(node, children);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);
        IMethodCaller methodCaller = findBinaryOperatorMethodCaller(methodName, types, bindingContext);

        if (methodCaller == null) {

            String message = errorMsg(methodName, types[0], types[1]);
            BindHelper.processError(message, node, bindingContext);

            return new ErrorBoundNode(node);
            //            throw new BoundError(errorMsg(methodName, types[0], types[1]), node);
        }

        return new BinaryOpNode(node, children, methodCaller);
    }

}
