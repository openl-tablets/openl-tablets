/*
 * Created on Jul 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class TypeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
     *      org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        
        ISyntaxNode typeNode = node.getChild(0);
        int dimension = 0;
        
        for (; !(typeNode instanceof IdentifierNode); ++dimension) {
            typeNode = typeNode.getChild(0);
        }

        String typeName = ((IdentifierNode) typeNode).getIdentifier();
        IOpenClass varType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (varType == null) {
            String message = String.format("Cannot bind node: '%s'. Cannot find type: '%s'.", 
                node.getModule().getCode(), typeName);
            BindHelper.processError(message, node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        if (dimension > 0) {
            varType = varType.getAggregateInfo().getIndexedAggregateType(varType, dimension);
        }
        BindHelper.checkOnDeprecation(node, bindingContext, varType);
        return new TypeBoundNode(node, varType);
    }

}
