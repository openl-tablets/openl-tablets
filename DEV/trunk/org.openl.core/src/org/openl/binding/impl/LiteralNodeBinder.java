/*
 * Created on May 29, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.LiteralNode;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class LiteralNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        String s = ((LiteralNode) node).getImage();

        if (s.equals("null")) {
            return new LiteralBoundNode(node, null, NullOpenClass.the);
        }

        if (s.equals("true")) {
            return new LiteralBoundNode(node, Boolean.TRUE, JavaOpenClass.BOOLEAN);
        }

        if (s.equals("false")) {
            return new LiteralBoundNode(node, Boolean.FALSE, JavaOpenClass.BOOLEAN);
        }

        String message = String.format("Literal <%s> can not be resolved", s);
        BindHelper.processError(message, node, bindingContext);

        return new ErrorBoundNode(node);
    }

}
