/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
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

public class UnaryOperatorNodeBinder extends ANodeBinder {

    static public String errorMsg(String methodName, IOpenClass t1) {
        return "Operator not defined: " + methodName + "(" + t1.getName() + ")";

    }

    static public IMethodCaller findUnaryOperatorMethodCaller(String methodName, IOpenClass[] types,
            IBindingContext bindingContext) {

        IMethodCaller om = bindingContext.findMethodCaller("org.openl.operators", methodName, types);

        if (om != null) {
            return om;
        }

        // IOpenClass[] types2 = { types[1] };

        om = MethodSearch.getMethodCaller(methodName, IOpenClass.EMPTY, bindingContext, types[0]);

        if (om != null) {
            return om;
        }

        om = MethodSearch.getMethodCaller(methodName, types, bindingContext, types[0]);

        return om;

    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode,
     *      org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 1) {
            throw new BoundError(node, "Unary node should have 1 subnode");
        }

        int index = node.getType().lastIndexOf('.');

        String methodName = node.getType().substring(index + 1);

        IBoundNode[] children = bindChildren(node, bindingContext);

        IOpenClass[] types = getTypes(children);

        IMethodCaller om = findUnaryOperatorMethodCaller(methodName, types, bindingContext);

        if (om == null) {
            throw new BoundError(node, errorMsg(methodName, types[0]));
        }

        return new UnaryOpNode(node, children, om);

    }

}
