package org.openl.binding.impl;

import java.math.BigDecimal;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.convertor.NumberNotation;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * Binds a number of thousands, millions or billions, such as {@code 5K}, {@code 2.5M} or {@code 3B}.
 *
 * <p>The suffix {@code K} multiplies the written number by 1000, {@code M} by 1000000 and {@code B} by 1000000000, as
 * in the bounds of a {@code DoubleRange}. The suffix is upper case only. Commas may group the thousands of the written
 * number, as in {@code 1,500K}.
 *
 * <p>The value is computed exactly. A whole value is an {@code int}, a {@code long} or a {@code BigInteger}, as a whole
 * number literal is, so {@code 2.5K} is the {@code int} 2500. A value with a fraction is a {@code double} rounded once,
 * so {@code 1.0005K} is 1000.5.
 *
 * @author Yury Molchan
 */
public class MultipliedNumberNodeBinder extends NumberLiteralNodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        var s = digits(node);
        var last = s.length() - 1;
        var multiplier = BigDecimal.valueOf(NumberNotation.multiplier(s.charAt(last)));
        var value = new BigDecimal(s.substring(0, last)).multiply(multiplier);
        if (value.remainder(BigDecimal.ONE).signum() == 0) {
            return wholeNumber(node, value.toBigInteger());
        }
        return new LiteralBoundNode(node, value.doubleValue(), JavaOpenClass.DOUBLE);
    }
}
