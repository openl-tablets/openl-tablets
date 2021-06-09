package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

public class BinaryOperatorAndNodeBinder extends BinaryOperatorNodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 2) {
            return makeErrorNode("Binary node must have 2 subnodes", node, bindingContext);
        }

        IBoundNode[] children = bindChildren(node, bindingContext);
        IBoundNode left = children[0];
        IBoundNode right = children[1];

        IOpenClass leftType = left.getType();
        IOpenClass rightType = right.getType();

        if ((leftType.getInstanceClass() == boolean.class || leftType.getInstanceClass() == Boolean.class) && (rightType
            .getInstanceClass() == boolean.class || rightType.getInstanceClass() == Boolean.class)) {

            return new BinaryOpNodeAnd(node, left, right);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);
        IMethodCaller methodCaller = findBinaryOperatorMethodCaller(methodName,
            new IOpenClass[] { leftType, rightType },
            bindingContext);

        if (methodCaller == null) {
            String message = errorMsg(methodName, leftType, rightType);
            return makeErrorNode(message, node, bindingContext);
        }

        return new BinaryOpNode(node, left, right, methodCaller);
    }

}
