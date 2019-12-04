/*
 * Created on Jul 1, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class ArrayInitializationBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return makeErrorNode("Array has always to be initialized with a type", node, bindingContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bindType(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext,
     * org.openl.types.IOpenClass)
     */
    @Override
    public IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) throws Exception {

        IOpenClass componentType = type.getAggregateInfo().getComponentType(type);
        if (componentType == null) {
            String message = String.format("Cannot convert an array into '%s'", type.getDisplayName(INamedThing.SHORT));
            return makeErrorNode(message, node, bindingContext);
        }

        IBoundNode[] nodes = bindTypeChildren(node, bindingContext, componentType);
        IOpenCast[] casts = new IOpenCast[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            casts[i] = getCast(nodes[i], componentType, bindingContext);
        }

        return new ArrayInitializerNode(node, nodes, type, casts);
    }

}
