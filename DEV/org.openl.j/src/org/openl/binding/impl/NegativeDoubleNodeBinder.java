package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * Node binder for negative double values. Supports <code>Double</code> and <code>Float</code>.
 *
 */
public class NegativeDoubleNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode[] children = bindChildren(node, bindingContext);

        LiteralBoundNode child = (LiteralBoundNode) children[0];

        Number value = (Number) child.getValue();

        if (value instanceof Double) {
            return new LiteralBoundNode(node, -value.doubleValue(), JavaOpenClass.DOUBLE);
        } else if (value instanceof Float) {
            return new LiteralBoundNode(node, -value.floatValue(), JavaOpenClass.FLOAT);
        }
        throw new RuntimeException("Unsupported double type: " + value.getClass());
    }

}
