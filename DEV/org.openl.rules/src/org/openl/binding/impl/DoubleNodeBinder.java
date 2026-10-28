/*
 * Created on Jun 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.math.BigDecimal;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * Binds a fractional number literal, such as {@code 1.5}, {@code 1.5e3}, {@code 2.5f} or {@code 1,000.00}.
 *
 * <p>Commas may group the thousands of the whole part. A literal that ends with {@code f} is a {@code float}. Other
 * literals are a {@code double}, or a {@code BigDecimal} when they are too large for a {@code double}.
 *
 * @author snshor
 */
public class DoubleNodeBinder extends NumberLiteralNodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        var s = digits(node);

        var len = s.length();

        if (Character.toUpperCase(s.charAt(len - 1)) == 'F') {
            return new LiteralBoundNode(node, Float.valueOf(s.substring(0, len - 1)), JavaOpenClass.FLOAT);
        }

        Double doubleValue = Double.parseDouble(s);
        if (!doubleValue.isInfinite() || doubleValue.toString().equals(s)) {
            return new LiteralBoundNode(node, doubleValue, JavaOpenClass.DOUBLE);
        }

        return new LiteralBoundNode(node, new BigDecimal(s), JavaOpenClass.getOpenClass(BigDecimal.class));
    }
}
