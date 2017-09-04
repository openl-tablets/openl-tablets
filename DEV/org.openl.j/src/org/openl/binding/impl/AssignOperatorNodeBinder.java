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
import org.openl.types.NullOpenClass;

/**
 * @author snshor
 */

public class AssignOperatorNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 2) {
            
            BindHelper.processError("Assign node must have 2 subnodes", node, bindingContext, false);
       
            return new ErrorBoundNode(node);
            //            throw new BoundError("Assign node must have 2 subnodes", node);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        if (!children[0].isLvalue()) {

            String message = String.format("The node '%s' is not an Lvalue", children[0].getClass().getName());
            BindHelper.processError(message, node, bindingContext, false);

            return new ErrorBoundNode(node);
            //            throw new BoundError(message,                children[0].getSyntaxNode());
        }

        IOpenClass[] types = getTypes(children);
        IOpenClass leftType = types[0];
        IMethodCaller methodCaller = null;

        if (!"assign".equals(methodName)) {

            methodCaller = BinaryOperatorNodeBinder.findBinaryOperatorMethodCaller(methodName, types, bindingContext);

            if (methodCaller == null) {

                String message = BinaryOperatorNodeBinder.errorMsg(methodName, types[0], types[1]);
                BindHelper.processError(message, node, bindingContext, false);

                return new ErrorBoundNode(node);
                //throw new BoundError(BinaryOperatorNodeBinder.errorMsg(methodName, types[0], types[1]), node);
            }
        }

        IOpenClass rightType = methodCaller == null ? types[1] : methodCaller.getMethod().getType();
        IOpenCast cast = null;

        if (!rightType.equals(leftType)) {

            cast = bindingContext.getCast(rightType, leftType);

            //only implicit casts and explicit casts for literal are allowed for right part
            if (cast == null || (!cast.isImplicit() && !(children[1] instanceof LiteralBoundNode))) {
            	if (!NullOpenClass.isAnyNull(rightType, leftType))
            	{	
            		String message = String.format("Can not convert from '%s' to '%s'",
                        rightType.getName(), leftType.getName());
                	BindHelper.processError(message, node, bindingContext, false);
            	}	

                return new ErrorBoundNode(node);
            }
        }

        return new AssignNode(node, children, methodCaller, cast);
    }

}
