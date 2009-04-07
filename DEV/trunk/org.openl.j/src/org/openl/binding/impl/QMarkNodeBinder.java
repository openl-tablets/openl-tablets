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
public class QMarkNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        // check condition

        IBoundNode conditionNode = children[0];
        if (conditionNode.getType() != JavaOpenClass.BOOLEAN) {
            throw new BoundError(conditionNode.getSyntaxNode(), "Condition must have boolean type");
        }

        if (children[1].getType() != children[2].getType()) {
            throw new BoundError(conditionNode.getSyntaxNode(),
                    "Both types should be the same in the conditional expression");
        }

        return new QMarkNode(node, children);
    }

}
