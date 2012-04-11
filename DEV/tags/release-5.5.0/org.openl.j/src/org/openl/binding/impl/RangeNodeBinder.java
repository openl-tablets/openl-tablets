/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.RangeWithBounds;

/**
 * @author snshor
 */
public class RangeNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);

        String type = node.getType();

        if (children[0] instanceof ErrorBoundNode) {
            return new ErrorBoundNode(node);
        }

        Number val = (Number) ((LiteralBoundNode) children[0]).getValue();

        if (type.contains("binary")) {

            Number val2 = (Number) ((LiteralBoundNode) children[1]).getValue();

            if (val.doubleValue() > val2.doubleValue()) {

                String message = String.format("%s must be more or equal than %s", val2.toString(), val.toString());
                BindHelper.processError(message, node, bindingContext);

                return new ErrorBoundNode(node);
            }

            if (type.endsWith("minus") || type.endsWith("ddot")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(val, val2),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));
            }

            if (type.endsWith("tdot")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(getMinimalIncrease(val), getMinimalDecrease(val2)),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));
            }
        }

        if (type.contains("number")) {
            return new LiteralBoundNode(node,
                new RangeWithBounds(val, val),
                JavaOpenClass.getOpenClass(RangeWithBounds.class));
        }

        if (type.contains("unary.prefix")) {

            if (type.endsWith("lt")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(getMin(val), getMinimalDecrease(val)),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));

            } else if (type.endsWith("le")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(getMin(val), val),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));

            } else if (type.endsWith("gt")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(getMinimalIncrease(val), getMax(val)),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));

            } else if (type.endsWith("ge")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(val, getMax(val)),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));
            }

            String message = String.format("Unsupported range prefix type: %s", type);
            BindHelper.processError(message, node, bindingContext);

            return new ErrorBoundNode(node);
        }

        if (type.contains("unary.suffix")) {

            if (type.endsWith("lt")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(getMinimalIncrease(val), getMax(val)),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));

            } else if (type.endsWith("le") || type.endsWith("plus")) {
                return new LiteralBoundNode(node,
                    new RangeWithBounds(val, getMax(val)),
                    JavaOpenClass.getOpenClass(RangeWithBounds.class));
            }

            String message = String.format("Unsupported range suffix type: %s", type);
            BindHelper.processError(message, node, bindingContext);

            return new ErrorBoundNode(node);
        }

        String message = String.format("Unsupported range type: %s", type);
        BindHelper.processError(message, node, bindingContext);

        return new ErrorBoundNode(node);
    }

    private Number getMax(Number number) {

        if (number.getClass() == Double.class) {
            return Double.POSITIVE_INFINITY;
        } else if (number.getClass() == Long.class) {
            return Long.MAX_VALUE;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    private Number getMin(Number number) {

        if (number.getClass() == Double.class) {
            return Double.NEGATIVE_INFINITY;
        } else if (number.getClass() == Long.class) {
            return Long.MIN_VALUE;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    private Number getMinimalDecrease(Number number) {

        if (number.getClass() == Double.class) {
            return number.doubleValue() - Math.abs(number.doubleValue() / 1e15);
        } else if (number.getClass() == Long.class) {
            return number.longValue() - 1;
        } else {
            return number.intValue() - 1;
        }
    }

    private Number getMinimalIncrease(Number number) {

        if (number.getClass() == Double.class) {
            return number.doubleValue() + Math.abs(number.doubleValue() / 1e15);
        } else if (number.getClass() == Long.class) {
            return number.longValue() + 1;
        } else {
            return number.intValue() + 1;
        }
    }

}
