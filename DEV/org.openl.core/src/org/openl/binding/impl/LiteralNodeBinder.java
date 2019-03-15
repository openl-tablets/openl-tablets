/*
 * Created on May 29, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class LiteralNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        String s = node.getText();

        if (s.equals("null")) {
            return new LiteralBoundNode(node, null, NullOpenClass.the);
        }

        if (s.equals("true")) {
            return new LiteralBoundNode(node, Boolean.TRUE, JavaOpenClass.BOOLEAN);
        }

        if (s.equals("false")) {
            return new LiteralBoundNode(node, Boolean.FALSE, JavaOpenClass.BOOLEAN);
        }

        if (s.equals("(") || s.equals(")") || s.equals("[") || s.equals("]")) {
            return new LiteralBoundNode(node, s.charAt(0), JavaOpenClass.CHAR);
        }

        return makeErrorNode("Literal <" + s + "> can not be resolved", node, bindingContext);
    }

}
