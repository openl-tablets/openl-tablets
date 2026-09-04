/*
 * Created on Jul 11, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 */
public class TypeCastBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);

        var to = children[0].getType();
        var from = children[1].getType();

        var cast = bindingContext.getCast(from, to);

        if (cast == null) {
            return makeErrorNode("Cannot convert from '%s' to '%s'.".formatted(from.getName(), to.getName()),
                    node,
                    bindingContext);
        }

        return new TypeCastNode(node, children[1], cast, to);
    }

}
