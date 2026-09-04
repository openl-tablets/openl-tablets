/*
 * Created on Jul 1, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 */
public class ListNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if ("list".equals(node.getType())) {
            IBoundNode[] children = bindChildren(node, bindingContext);

            String[] res = new String[children.length];

            for (var i = 0; i < res.length; i++) {
                res[i] = (String) ((LiteralBoundNode) children[i]).getValue();
            }

            return new LiteralBoundNode(node, res, JavaOpenClass.getOpenClass(String[].class));
        }

        var sb = new StringBuilder();

        for (var i = 0; i < node.getNumberOfChildren(); i++) {

            var child = node.getChild(i);

            if (i > 0) {
                sb.append(' ');
            }

            sb.append(((IdentifierNode) child).getIdentifier());
        }

        return new LiteralBoundNode(node, sb.toString(), JavaOpenClass.STRING);
    }

}
