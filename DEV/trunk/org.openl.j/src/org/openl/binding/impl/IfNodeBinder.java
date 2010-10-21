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
public class IfNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        IBoundNode conditionNode = children[0];

        if (conditionNode != null && conditionNode.getType() != JavaOpenClass.BOOLEAN && conditionNode.getType() != JavaOpenClass.getOpenClass(Boolean.class)) {
            BindHelper.processError("Condition must have boolean type", conditionNode.getSyntaxNode(), bindingContext);

            return new ErrorBoundNode(node);
        }

        return new IfNode(node, children);
    }

}
