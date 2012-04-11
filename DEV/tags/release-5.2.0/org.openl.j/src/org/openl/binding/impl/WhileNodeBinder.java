/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
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
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
     *      org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);

        // check condition
        IBoundNode conditionNode = children[0];
        if (conditionNode != null && !conditionNode.getType().equals(JavaOpenClass.BOOLEAN)) {
            throw new BoundError(conditionNode.getSyntaxNode(), "While: condition must have boolean type");
        }

        return new WhileNode(node, children);
    }

}
