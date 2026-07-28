/**
 *
 */
package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.java.JavaOpenClass;

public class BusinessIntNodeBinder extends BusinessNumberNodeBinder {

    private static int getIntValue(Long number, ISyntaxNode node) throws SyntaxNodeException {
        if (number > Integer.MAX_VALUE || number < Integer.MIN_VALUE) {
            var message = "Number %d is outside the valid range %d - %d"
                    .formatted(number, Integer.MIN_VALUE, Integer.MAX_VALUE);
            throw SyntaxNodeExceptionUtils.createError(message, node);
        }

        return number.intValue();
    }

    @Override
    protected IBoundNode makeNumber(String literal, int multiplier, ISyntaxNode node) throws SyntaxNodeException {
        final var FRACTION_DELIMITER = '.';

        long parsedNumber;
        if (literal.indexOf(FRACTION_DELIMITER) >= 0) {
            var x = Double.parseDouble(literal) * multiplier;
            parsedNumber = Math.round(x);
        } else {
            parsedNumber = Long.parseLong(literal) * multiplier;
        }

        var number = getIntValue(parsedNumber, node);

        return new LiteralBoundNode(node, number, JavaOpenClass.INT);
    }
}
