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
 * @author snshor
 */
public class DoubleNodeBinder extends NumberLiteralNodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        var s = node.getText();

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
