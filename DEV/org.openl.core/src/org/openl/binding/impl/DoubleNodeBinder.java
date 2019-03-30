/*
 * Created on Jun 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.math.BigDecimal;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class DoubleNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        String s = node.getText();

        int len = s.length();

        if (Character.toUpperCase(s.charAt(len - 1)) == 'F') {
            return new LiteralBoundNode(node, Double.valueOf(s.substring(0, len - 1)), JavaOpenClass.DOUBLE);
        }

        Double doubleValue = Double.valueOf(s);
        if (!doubleValue.isInfinite() || doubleValue.toString().equals(s)) {
            return new LiteralBoundNode(node, doubleValue, JavaOpenClass.DOUBLE);
        }

        return new LiteralBoundNode(node, new BigDecimal(s), JavaOpenClass.getOpenClass(BigDecimal.class));
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
