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

        IBoundNode target = children[0];
        IBoundNode source = children[1];
        if (!target.isLvalue()) {
            return makeErrorNode("Impossible to assign value", node, bindingContext);
        }

        IOpenClass targetType = target.getType();
        IOpenClass sourceType = source.getType();
        IMethodCaller methodCaller = null;

        if (!"assign".equals(methodName)) {

            methodCaller = BinaryOperatorNodeBinder.findBinaryOperatorMethodCaller(methodName, new IOpenClass[]{targetType, sourceType}, bindingContext);

            if (methodCaller == null) {

                String message = BinaryOperatorNodeBinder.errorMsg(methodName, targetType, sourceType);
                return makeErrorNode(message, node, bindingContext);
            }
        }

        IOpenClass rightType = methodCaller == null ? sourceType : methodCaller.getMethod().getType();
        IOpenCast cast = null;

        if (!rightType.equals(targetType)) {

            cast = bindingContext.getCast(rightType, targetType);

            // only implicit casts and explicit casts for literal are allowed for right part
            if (cast == null || !cast.isImplicit() && !(source instanceof LiteralBoundNode)) {
                String message = String
                    .format("Cannot convert from '%s' to '%s'.", rightType.getName(), targetType.getName());
                return makeErrorNode(message, node, bindingContext);
            }
        }

        /*
         * target = source - simple assign
         * target += source - assign with operation through methodCaller
         */
        return new AssignNode(node, target, source, methodCaller, cast);
    }

}
