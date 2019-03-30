package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * A binder for Type declaration. Like
 *
 * <li>List</li>
 * <li>String[]</li>
 * <li>int[][][]</li>
 *
 * @author Yury Molchan
 *
 */
public class TypeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        ISyntaxNode typeNode = node.getChild(0);

        IOpenClass varType = getType(typeNode, bindingContext);

        return new TypeBoundNode(node, varType);
    }
}
