/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class AssignOperatorNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 2) {
            return makeErrorNode("Assign node must have 2 subnodes", node, bindingContext);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        if (!children[0].isLvalue()) {
            return makeErrorNode("Impossible to assign value", node, bindingContext);
        }

        IOpenClass[] types = getTypes(children);
        IOpenClass leftType = types[0];
        IMethodCaller methodCaller = null;

        if (!"assign".equals(methodName)) {

            methodCaller = BinaryOperatorNodeBinder.findBinaryOperatorMethodCaller(methodName, types, bindingContext);

            if (methodCaller == null) {

                String message = BinaryOperatorNodeBinder.errorMsg(methodName, types[0], types[1]);
                return makeErrorNode(message, node, bindingContext);
            }
        }

        IOpenClass rightType = methodCaller == null ? types[1] : methodCaller.getMethod().getType();
        IOpenCast cast = null;

        if (!rightType.equals(leftType)) {

            cast = bindingContext.getCast(rightType, leftType);

            // only implicit casts and explicit casts for literal are allowed for right part
            if (cast == null || (!cast.isImplicit() && !(children[1] instanceof LiteralBoundNode))) {
                String message = "Cannot convert from '" + rightType.getName() + "' to '" + leftType.getName() + "'";
                return makeErrorNode(message, node, bindingContext);
            }
        }

        return new AssignNode(node, children, methodCaller, cast);
    }

}
