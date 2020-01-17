package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
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
    protected IBoundNode createBoundNode(ISyntaxNode node,
                                         IBoundNode targetNode,
                                         IBoundNode expressionNode,
                                         ILocalVar localVar,
                                         IBindingContext bindingContext) {
        if (expressionNode.getType() == NullOpenClass.the) {
            String message = "Null cannot be used as expression for array index operator.";
            return makeErrorNode(message, expressionNode.getSyntaxNode(), bindingContext);
        }
        boolean isUnique = node.getType().contains("unique");
        if (isUnique) {
            return new TransformToUniqueIndexNode(node, targetNode, expressionNode, localVar);
        } else {
            return new TransformIndexNode(node, targetNode, expressionNode, localVar);
        }
    }
}
