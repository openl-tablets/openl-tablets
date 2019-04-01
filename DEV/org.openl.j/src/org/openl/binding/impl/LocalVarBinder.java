/*
 * Created on Jun 14, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class LocalVarBinder extends ANodeBinder {

    public static final IBoundNode createLocalVarDeclarationNode(ISyntaxNode node,
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

        ILocalVar var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, name, varType);

        return new LocalVarDeclarationNode(node, init == null ? null : new IBoundNode[] { init }, var);
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode typeNode = bindChildNode(node.getChild(0), bindingContext);
        IOpenClass varType = typeNode.getType();

        List<IBoundNode> boundNodes = new ArrayList<>();

        for (int i = 1; i < node.getNumberOfChildren(); ++i) {

            // we may get basically 2 different situations here, either just
            // name or name and initializer
            ISyntaxNode child = node.getChild(i);

            if (child instanceof IdentifierNode) {
                String name = child.getText();
                boundNodes.add(createLocalVarDeclarationNode(child, name, null, varType, bindingContext, false));
            } else {
                String name = child.getChild(0).getText();
                boundNodes
                    .add(createLocalVarDeclarationNode(child, name, child.getChild(1), varType, bindingContext, false));
            }
        }

        return new BlockNode(node, 0, (boundNodes.toArray(new IBoundNode[boundNodes.size()])));
    }

}
