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

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return bindChildNode(toIdentifierNode(node), bindingContext);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) {
        return bindTargetNode(toIdentifierNode(node), bindingContext, target);
    }

    public static IdentifierNode toIdentifierNode(ISyntaxNode node) {
        String longName = concatChildren(node);
        return new IdentifierNode("identifier.nostrict", node.getSourceLocation(), longName, node.getModule());
    }

    private static String concatChildren(ISyntaxNode node) {

        StringBuilder builder = new StringBuilder(100);

        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            builder.append(((IdentifierNode) node.getChild(i)).getIdentifier());
        }

        return builder.toString();
    }

}
