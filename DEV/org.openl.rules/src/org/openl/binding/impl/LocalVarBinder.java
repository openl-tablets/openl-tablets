/*
 * Created on Jun 14, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;

import org.jspecify.annotations.Nullable;

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

    /**
     * Declares a local variable and binds the value it starts with.
     * <p>
     * The declaration is the name alone, or the name and the value the variable starts with.
     *
     * @param varType  the type of the variable, or {@code null} to take the type of the value it starts with
     * @param typeNode the type written in the declaration, for the hint and the link of the editor. It is never
     *                 evaluated. {@code null} when the type is not written, or when another variable of the same
     *                 statement already keeps it.
     */
    static IBoundNode createLocalVarDeclarationNode(ISyntaxNode node,
                                                    @Nullable IOpenClass varType,
                                                    @Nullable IBoundNode typeNode,
                                                    IBindingContext bindingContext) {
        var nameOnly = node instanceof IdentifierNode;
        var name = nameOnly ? node.getText() : node.getChild(0).getText();
        IBoundNode init = null;
        if (!nameOnly) {
            var initializationNode = node.getChild(1);
            if (varType == null) {
                init = bindChildNode(initializationNode, bindingContext);
                varType = init.getType();
            } else {
                init = bindTypeNode(initializationNode, bindingContext, varType);
            }
        }

        var localVar = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, name, varType);

        return new LocalVarDeclarationNode(node, typeNode, init, localVar);
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode typeNode = bindChildNode(node.getChild(0), bindingContext);
        var varType = typeNode.getType();
        // The editor shows the type once, with the first variable of the statement. Rule Services do not keep it.
        var shownType = bindingContext.isExecutionMode() ? null : typeNode;

        var boundNodes = new ArrayList<IBoundNode>();

        for (var i = 1; i < node.getNumberOfChildren(); i++) {
            boundNodes.add(createLocalVarDeclarationNode(node.getChild(i),
                    varType,
                    i == 1 ? shownType : null,
                    bindingContext));
        }

        return new BlockNode(node, 0, boundNodes.toArray(IBoundNode.EMPTY));
    }

}
