package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author Yury Molchan
 */
public class ForNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);
        var initNode = children[0];
        var conditionNode = children[1];
        var afterNode = children[2];
        var blockCodeNode = children[3];

        IBoundNode checkConditionNode = BindHelper.checkConditionBoundNode(conditionNode, bindingContext);

        if (checkConditionNode != conditionNode) {
            return checkConditionNode;
        }

        return new LoopNode(node, initNode, checkConditionNode, blockCodeNode, afterNode);
    }
}
