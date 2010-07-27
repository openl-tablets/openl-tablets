/**
 *
 */
package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.java.JavaOpenClass;

/**
 *
 */
public class BusinessDoubleNodeBinder extends BusinessNumberNodeBinder {
    @Override
    protected IBoundNode makeNumber(String literal, int multiplier, ISyntaxNode node) throws SyntaxNodeException {
        Double value = Double.parseDouble(literal) * multiplier;
        return new LiteralBoundNode(node, value, JavaOpenClass.DOUBLE);
    }
}
