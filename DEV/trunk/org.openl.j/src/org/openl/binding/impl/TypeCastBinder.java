/*
 * Created on Jul 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class TypeCastBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
     *      org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode[] children = bindChildren(node, bindingContext);

        IOpenClass to = children[0].getType();
        IOpenClass from = children[1].getType();

        if (to.equals(from)) {
            return children[1];
        }

        IOpenCast cast = bindingContext.getCast(from, to);

        if (cast == null) {
            throw new BoundError(node, "Can not convert from " + from.getName() + " to " + to.getName());
        }

        return new CastNode(node, children[1], cast, to);
    }

}
