/*
 * Created on Jun 16, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class WhileNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        IBoundNode conditionNode = children[0];

        if (conditionNode != null && !conditionNode.getType().equals(JavaOpenClass.BOOLEAN)) {
            BindHelper.processError("'While' condition must have boolean type",
                conditionNode.getSyntaxNode(),
                bindingContext);

            return new ErrorBoundNode(node);
        }

        return new WhileNode(node, children);
    }

}
