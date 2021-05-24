package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.impl.ParameterDeclaration;

public class ParameterDeclarationNodeBinder extends ANodeBinder {

    private static final int TYPE_NODE = 0;
    private static final int NAME_NODE = 1;

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode typeNode = bindChildNode(node.getChild(TYPE_NODE), bindingContext);
        String name = null;
        if (node.getNumberOfChildren() == 2) {
            name = ((IdentifierNode) node.getChild(NAME_NODE)).getIdentifier();
        }
        return new ParameterDeclarationNode(node, new ParameterDeclaration(typeNode.getType(), name));
    }

}
