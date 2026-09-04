/*
 * Created on Jun 14, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */
public class LocalVarBinder extends ANodeBinder {

    public static IBoundNode createLocalVarDeclarationNode(ISyntaxNode node,
                                                           String name,
                                                           ISyntaxNode initializationNode,
                                                           IOpenClass varType,
                                                           IBindingContext bindingContext,
                                                           boolean implyExpressionType) {
        IBoundNode init = null;

        if (initializationNode != null) {
            if (implyExpressionType) {
                init = bindChildNode(initializationNode, bindingContext);
                varType = init.getType();
            } else {
                init = bindTypeNode(initializationNode, bindingContext, varType);
            }
        }

        var var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, name, varType);

        return new LocalVarDeclarationNode(node, init, var);
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode typeNode = bindChildNode(node.getChild(0), bindingContext);
        var varType = typeNode.getType();

        var boundNodes = new ArrayList<IBoundNode>();

        for (var i = 1; i < node.getNumberOfChildren(); ++i) {

            // we may get basically 2 different situations here, either just
            // name or name and initializer
            var child = node.getChild(i);

            if (child instanceof IdentifierNode) {
                var name = child.getText();
                boundNodes.add(createLocalVarDeclarationNode(child, name, null, varType, bindingContext, false));
            } else {
                var name = child.getChild(0).getText();
                boundNodes
                        .add(createLocalVarDeclarationNode(child, name, child.getChild(1), varType, bindingContext, false));
            }
        }

        return new BlockNode(node, 0, boundNodes.toArray(IBoundNode.EMPTY));
    }

}
