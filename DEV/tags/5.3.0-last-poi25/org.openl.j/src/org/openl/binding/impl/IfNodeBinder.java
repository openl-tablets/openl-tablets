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
public class IfNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        // check condition

        IBoundNode conditionNode = children[0];
        if (conditionNode.getType() != JavaOpenClass.BOOLEAN) {
            throw new BoundError(conditionNode.getSyntaxNode(), "Condition must have boolean type");
        }

        return new IfNode(node, children);
    }

}
