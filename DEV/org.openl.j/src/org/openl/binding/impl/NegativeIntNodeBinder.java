package org.openl.binding.impl;

import java.math.BigInteger;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

public class NegativeIntNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode[] children = bindChildren(node, bindingContext);

        LiteralBoundNode child = (LiteralBoundNode) children[0];

        Number value = (Number) child.getValue();

        if (value instanceof Integer) {
            return new LiteralBoundNode(node, -value.intValue(), JavaOpenClass.INT);
        } else if (value instanceof Long) {
            return new LiteralBoundNode(node, -value.longValue(), JavaOpenClass.LONG);
        } else if (value instanceof BigInteger) {
            return new LiteralBoundNode(node,
                ((BigInteger) value).negate(),
                JavaOpenClass.getOpenClass(BigInteger.class));
        }
        throw new RuntimeException("Unsupported integer type: " + value.getClass());
    }

}
