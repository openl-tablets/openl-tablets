package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author Yury Molchan
 * 
 */
public class ForNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        IBoundNode initNode = children[0];
        IBoundNode conditionNode = children[1];
        IBoundNode afterNode = children[2];
        IBoundNode blockCodeNode = children[3];

        IBoundNode checkConditionNode = BindHelper.checkConditionBoundNode(conditionNode, bindingContext);

        if (checkConditionNode != conditionNode) {
            return checkConditionNode;
        }

        return new LoopNode(node, initNode, checkConditionNode, blockCodeNode, afterNode);
    }
}
