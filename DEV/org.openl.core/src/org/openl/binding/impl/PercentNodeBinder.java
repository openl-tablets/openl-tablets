/*
 * Created on Jun 6, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class PercentNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        String s = node.getText();

        int len = s.length();

        if (Character.toUpperCase(s.charAt(len - 1)) == 'F') {
            return new LiteralBoundNode(node, Float.valueOf(s.substring(0, len - 1)), JavaOpenClass.FLOAT);
        }

        return new LiteralBoundNode(node, Double.valueOf(s.substring(0, len - 1)) / 100, JavaOpenClass.DOUBLE);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode)
        throws Exception {

        IBoundNode thisNode = bind(node, bindingContext);

        return BinaryOperatorNodeBinder.bindOperator(node, "multiply", targetNode, thisNode, bindingContext);

    }

}
