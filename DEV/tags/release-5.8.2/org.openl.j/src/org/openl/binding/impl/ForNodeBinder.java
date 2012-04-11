/*
 * Created on Jun 16, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.OpenClassHelper;

/**
 * @author snshor
 * 
 */
public class ForNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        IBoundNode conditionNode = children[1];

        if (conditionNode != null && !OpenClassHelper.isBooleanType(conditionNode.getType())) {
            BindHelper.processError("Condition must have boolean type", conditionNode.getSyntaxNode(), bindingContext);
            return new ErrorBoundNode(node);
        }

        return new ForNode(node, children);
    }

}
