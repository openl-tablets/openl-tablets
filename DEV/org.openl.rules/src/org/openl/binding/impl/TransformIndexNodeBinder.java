package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.NullOpenClass;

/**
 * Binds conditional index for arrays like: - arrayOfDrivers[@ age < 20]; - arrayOfDrivers[select all having gender ==
 * "Male"]
 *
 * @author PUdalau
 */
public class TransformIndexNodeBinder extends BaseAggregateIndexNodeBinder {

    @Override
    IBoundNode createBoundNode(ISyntaxNode node,
                               IBoundNode targetNode,
                               IBoundNode expressionNode,
                               ElementVariables variables,
                               IBindingContext bindingContext) {
        if (expressionNode.getType() == NullOpenClass.the) {
            var message = "Null cannot be used as expression for array index operator.";
            return makeErrorNode(message, expressionNode.getSyntaxNode(), bindingContext);
        }
        var isUnique = node.getType().contains("unique");
        return new TransformIndexNode(node, targetNode, expressionNode, variables, isUnique);
    }
}
