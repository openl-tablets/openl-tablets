package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;

/**
 * Binds conditional index for arrays that returns the first matching element like: - arrayOfDrivers[@ age < 20]; -
 * arrayOfDrivers[select all having gender == "Male"]
 *
 * @author PUdalau
 */
public class SelectFirstIndexNodeBinder extends BaseAggregateIndexNodeBinder {

    @Override
    protected IBoundNode createBoundNode(ISyntaxNode node,
            IBoundNode targetNode,
            IBoundNode expressionNode,
            ILocalVar localVar,
            IBindingContext bindingContext) {
        expressionNode = BindHelper.checkConditionBoundNode(expressionNode, bindingContext);
        return new SelectFirstIndexNode(node, targetNode, expressionNode, localVar);
    }
}
