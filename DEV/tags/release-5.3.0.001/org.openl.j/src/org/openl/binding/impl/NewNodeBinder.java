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

public class NewNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode,
     *      org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int nc = node.getNumberOfChildren();
        if (nc < 1) {
            throw new BoundError(node, "New node must have at least one subnode");
        }

        ISyntaxNode typeNode = node.getChild(0);

        String typeName = ((IdentifierNode) typeNode).getIdentifier();

        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (type == null) {
            throw new BoundError(typeNode, "Type " + typeName + " not found");
        }

        IBoundNode[] children = bindChildren(node, bindingContext, 1, nc);

        IOpenClass[] types = getTypes(children);

        IMethodCaller om = MethodSearch.getMethodCaller(type.getName(), types, bindingContext, type);
        // IMethodCaller om =
        // bindingContext.findMethodCaller("org.openl.this",t, types);

        if (om == null) {
            String errMsg = "Constructor not found: " + MethodUtil.printMethod(type.getName(), types);
            throw new BoundError(node, errMsg);
        }

        return new MethodBoundNode(node, children, om);

    }

}
