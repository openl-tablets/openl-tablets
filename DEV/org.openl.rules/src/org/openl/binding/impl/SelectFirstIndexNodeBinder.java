package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * Binds conditional index for arrays that returns the first matching element like: - arrayOfDrivers[@ age < 20]; -
 * arrayOfDrivers[select all having gender == "Male"]
 *
 * @author PUdalau
 */
public class SelectFirstIndexNodeBinder extends BaseAggregateIndexNodeBinder {

    @Override
    IBoundNode createBoundNode(ISyntaxNode node,
                               IBoundNode targetNode,
                               IBoundNode expressionNode,
                               ElementVariables variables,
                               IBindingContext bindingContext) {
        expressionNode = BindHelper.checkConditionBoundNode(expressionNode, bindingContext);
        return new SelectFirstIndexNode(node, targetNode, expressionNode, variables);
    }
}
