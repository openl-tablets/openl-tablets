/*
 * Created on Jul 11, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
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

        IOpenClass to = children[0].getType();
        IOpenClass from = children[1].getType();
        
        if (to.equals(from)) {
            return children[1];
        }

        IOpenCast cast = bindingContext.getCast(from, to);

        if (cast == null) {
            return makeErrorNode(String.format("Cannot convert from '%s' to '%s'.", from.getName(), to.getName()),
                node,
                bindingContext);
        }

        return new CastNode(node, children[1], cast, to);
    }

}
