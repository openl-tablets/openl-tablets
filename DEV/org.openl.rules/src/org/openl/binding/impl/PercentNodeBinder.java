/*
 * Created on Jun 6, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.math.BigDecimal;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * Binds a percent literal, such as {@code 15%}, {@code 0.5%} or {@code 1.5e2%}, to a {@code double}.
 *
 * <p>The value is the written number divided by 100 and rounded once, so {@code 0.07%} equals {@code 0.0007}.
 *
 * @author snshor
 */
public class PercentNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        var s = node.getText();

        var number = new BigDecimal(s.substring(0, s.length() - 1));

        return new LiteralBoundNode(node, number.movePointLeft(2).doubleValue(), JavaOpenClass.DOUBLE);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node,
                                 IBindingContext bindingContext,
                                 IBoundNode targetNode) throws Exception {

        var thisNode = bind(node, bindingContext);

        return BinaryOperatorNodeBinder.bindOperator(node, "multiply", targetNode, thisNode, bindingContext);

    }

}
