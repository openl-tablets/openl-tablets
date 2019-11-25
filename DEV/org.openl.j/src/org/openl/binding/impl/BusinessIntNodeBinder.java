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

    private int getIntValue(Long number, ISyntaxNode node) throws SyntaxNodeException {
        if (number > Integer.MAX_VALUE || number < Integer.MIN_VALUE) {
            String message = String
                .format("Number %d is outside the valid range %d - %d", number, Integer.MIN_VALUE, Integer.MAX_VALUE);
            throw SyntaxNodeExceptionUtils.createError(message, node);
        }

        return number.intValue();
    }

    @Override
    protected IBoundNode makeNumber(String literal, int multiplier, ISyntaxNode node) throws SyntaxNodeException {
        final char FRACTION_DELIMITER = '.';

        Long parsedNumber;
        if (literal.indexOf(FRACTION_DELIMITER) >= 0) {
            Double x = Double.parseDouble(literal) * multiplier;
            parsedNumber = Math.round(x);
        } else {
            parsedNumber = Long.parseLong(literal) * multiplier;
        }

        int number = getIntValue(parsedNumber, node);

        return new LiteralBoundNode(node, number, JavaOpenClass.INT);
    }
}
