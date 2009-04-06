/**
 * 
 */
package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.java.JavaOpenClass;

/**
 *
 */
public class BusinessIntNodeBinder extends BusinessNumberNodeBinder {

    protected IBoundNode makeNumber(String literal, int multiplier, ISyntaxNode node) throws SyntaxError {
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

    private int getIntValue(Long number, ISyntaxNode node) throws SyntaxError {
         if (number > Integer.MAX_VALUE || number < Integer.MIN_VALUE) {
             String message = String.format("Number %d is outside the valid range %d - %d", number.longValue(), Integer.MIN_VALUE, Integer.MAX_VALUE);
             throw new SyntaxError(node, message, null);
         }

         return number.intValue();
    }
}
