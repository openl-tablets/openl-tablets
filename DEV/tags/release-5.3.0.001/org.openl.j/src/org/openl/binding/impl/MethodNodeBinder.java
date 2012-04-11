/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class MethodNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode,
     *      org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int nc = node.getNumberOfChildren();
        if (nc < 1) {
            throw new BoundError(node, "Method node should have at least one subnode");
        }

        ISyntaxNode lastNode = node.getChild(nc - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, nc - 1);

        IOpenClass[] types = getTypes(children);

        IMethodCaller om = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, types);

        if (om == null) {
            throw new BoundError(node, "Method " + MethodUtil.printMethod(methodName, types) + " not found");
        }

        return new MethodBoundNode(node, children, om);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bindTarget(org.openl.syntax.ISyntaxNode,
     *      org.openl.binding.IBindingContext, org.openl.types.IOpenClass)
     */
    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) throws Exception {
        int nc = node.getNumberOfChildren();
        if (nc < 1) {
            throw new BoundError(node, "New node should have at least one subnode");
        }

        ISyntaxNode lastNode = node.getChild(nc - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, nc - 1);

        IOpenClass[] types = getTypes(children);

        IMethodCaller om = MethodSearch.getMethodCaller(methodName, types, bindingContext, target.getType());

        if (om == null) {
            StringBuffer buf = new StringBuffer("Method ");
            MethodUtil.printMethod(methodName, types, buf);
            buf.append(" not found in '" + target.getType().getName() + "'");

            throw new BoundError(node, buf.toString(), null);
        }

        MethodBoundNode res = new MethodBoundNode(node, children, om, target);
        res.setTargetNode(target);
        return res;

    }

}
