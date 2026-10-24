/*
 * Created on May 29, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.math.BigInteger;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Binds a whole number literal.
 *
 * <p>A number is decimal even when it starts with zeros, so {@code 010} is ten. A {@code 0x} prefix makes it
 * hexadecimal. There are no octal numbers.
 *
 * <p>The literal is an {@code int} when it fits, a {@code long} when it fits or ends with {@code L}, and a
 * {@code BigInteger} otherwise.
 *
 * @author snshor
 */
public class IntNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        var s = node.getText();

        if (s.charAt(0) == '+') {
            s = s.substring(1);
        }

        var isLong = Character.toUpperCase(s.charAt(s.length() - 1)) == 'L';
        if (isLong) {
            s = s.substring(0, s.length() - 1);
        }

        var radix = 10;
        if (s.length() > 2 && Character.toUpperCase(s.charAt(1)) == 'X') {
            radix = 16;
            s = s.substring(2);
        }

        if (isLong) {
            return new LiteralBoundNode(node, Long.parseLong(s, radix), JavaOpenClass.LONG);
        }

        var value = new BigInteger(s, radix);
        if (value.bitLength() < Integer.SIZE) {
            return new LiteralBoundNode(node, value.intValue(), JavaOpenClass.INT);
        } else if (value.bitLength() < Long.SIZE) {
            return new LiteralBoundNode(node, value.longValue(), JavaOpenClass.LONG);
        }
        return new LiteralBoundNode(node, value, JavaOpenClass.getOpenClass(BigInteger.class));
    }

    @Override
    public IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) throws Exception {
        IBoundNode boundNode = bindChildNode(node, bindingContext);
        IOpenCast cast = getCast(boundNode, type, bindingContext, false);

        if (cast == null) {
            return boundNode;
        }

        return new CastNode(null, boundNode, cast, type);
    }
}
