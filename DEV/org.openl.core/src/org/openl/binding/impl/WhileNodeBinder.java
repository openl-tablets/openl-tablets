package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author Yury Molchan
 *
 */
public class WhileNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        IBoundNode conditionNode = children[0];
        IBoundNode blockCodeNode = children[1];

        IBoundNode checkConditionNode = BindHelper.checkConditionBoundNode(conditionNode, bindingContext);

        if (checkConditionNode != conditionNode) {
            return checkConditionNode;
        }

        return new LoopNode(node, conditionNode, blockCodeNode);
    }
}
