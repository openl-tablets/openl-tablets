/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;

/**
 * @author snshor
 */
public class IdentifierSequenceBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        String longName = concatChildren(node);

        IdentifierNode newNode = new IdentifierNode("identifier.nostrict",
            node.getSourceLocation(),
            longName,
            node.getModule());

        return bindChildNode(newNode, bindingContext);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) {

        try {
            String longName = concatChildren(node);

            IdentifierNode newNode = new IdentifierNode("identifier.nostrict",
                node.getSourceLocation(),
                longName,
                node.getModule());

            return bindTargetNode(newNode, bindingContext, target);

        } catch (Exception | LinkageError e) {
            return makeErrorNode("Cannot bind node", e, node, bindingContext);
        } 
    }

    private String concatChildren(ISyntaxNode node) {

        StringBuilder builder = new StringBuilder(100);

        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            builder.append(((IdentifierNode) node.getChild(i)).getIdentifier());
        }

        return builder.toString();
    }

}
