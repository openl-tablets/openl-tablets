package org.openl.binding.impl;

import java.math.BigInteger;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Binds a number literal. A literal bound to a type is cast to it when a cast exists, and is kept as is otherwise.
 *
 * @author Yury Molchan
 */
abstract class NumberLiteralNodeBinder extends ANodeBinder {

    /**
     * Returns the literal as written, without the commas that group its thousands. The grammar accepts a comma only
     * between groups of three digits.
     */
    static String digits(ISyntaxNode node) {
        return node.getText().replace(",", "");
    }

    /**
     * Binds a whole number to an {@code int} when it fits, to a {@code long} when it fits, and to a
     * {@code BigInteger} otherwise.
     */
    static IBoundNode wholeNumber(ISyntaxNode node, BigInteger value) {
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
