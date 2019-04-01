package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObjectField;

/*
 * Created on Sep 23, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

/**
 * @author snshor
 *
 */
public class VarDeclarationNodeBinder extends ANodeBinder {

    private final IBoundNode createVarDeclarationNode(ISyntaxNode node,
            String name,
            ISyntaxNode initializationNode,
            IOpenClass varType,
            IBindingContext bindingContext) throws Exception {

        IBoundNode init = null;
        IOpenCast cast = null;

        if (initializationNode != null) {
            init = bindTypeNode(initializationNode, bindingContext, varType);
            cast = getCast(init, varType, bindingContext);
        }

        return new VarDeclarationNode(node,
            init == null ? null : new IBoundNode[] { init },
            new DynamicObjectField(name, varType),
            cast);
    }

    @Override
    public IBoundNode bind(ISyntaxNode unode, IBindingContext bindingContext) throws Exception {

        ISyntaxNode node = unode.getChild(0);
        IBoundNode typeNode = bindChildNode(node.getChild(0), bindingContext);
        IOpenClass varType = typeNode.getType();

        List<IBoundNode> boundNodes = new ArrayList<>();

        for (int i = 1; i < node.getNumberOfChildren(); ++i) {
            // we may get basically 2 different situations here, either just
            // name or name and initializer
            ISyntaxNode child = node.getChild(i);

            if (child instanceof IdentifierNode) {
                String name = child.getText();
                boundNodes.add(createVarDeclarationNode(child, name, null, varType, bindingContext));

            } else {
                String name = child.getChild(0).getText();
                boundNodes.add(createVarDeclarationNode(child, name, child.getChild(1), varType, bindingContext));
            }
        }

        return new MemberBlockNode(node, (boundNodes.toArray(new IBoundNode[boundNodes.size()])));
    }

}
