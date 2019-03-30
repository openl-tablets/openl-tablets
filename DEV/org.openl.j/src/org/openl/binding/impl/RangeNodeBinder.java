/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * @author snshor
 */
public class RangeNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = bindChildren(node, bindingContext);

        for (IBoundNode child : children) {
            if (child instanceof ErrorBoundNode) {
                return new ErrorBoundNode(node);
            }
        }

        RangeWithBounds range = null;

        String type = node.getType();
        if (type.contains("brackets")) {
            range = bindBrackets(children);
        } else if (type.contains("binary")) {
            range = bindBinary(children, type);
        } else if (type.contains("number")) {
            range = bindNumber(children);
        } else if (type.contains("unary.prefix")) {
            range = bindPrefix(children, type);
        } else if (type.contains("unary.suffix")) {
            range = bindSuffix(children, type);
        }

        if (range == null) {
            return makeErrorNode("Unsupported range type: " + type, node, bindingContext);
        }

        return new LiteralBoundNode(node, range, JavaOpenClass.getOpenClass(RangeWithBounds.class));
    }

    private RangeWithBounds bindBrackets(IBoundNode[] children) {
        int minBoundIndex = -1;
        int maxBoundIndex = -1;

        Number firstNumber = null;
        Number secondNumber = null;

        BoundType leftBoundType = null;
        BoundType rightBoundType = null;

        for (int i = 0; i < children.length; i++) {
            Object value = ((LiteralBoundNode) children[i]).getValue();
            if (value instanceof Number) {
                if (firstNumber == null) {
                    firstNumber = (Number) value;
                } else {
                    secondNumber = (Number) value;
                }
            } else if (value.equals('[') || value.equals('(')) {
                minBoundIndex = i;
                leftBoundType = value.equals('(') ? BoundType.EXCLUDING : BoundType.INCLUDING;
            } else if (value.equals(']') || value.equals(')')) {
                maxBoundIndex = i;
                rightBoundType = value.equals(')') ? BoundType.EXCLUDING : BoundType.INCLUDING;
            }
        }

        if (minBoundIndex == maxBoundIndex || minBoundIndex < 0 || maxBoundIndex < 0) {
            throw new OpenLRuntimeException("Incorrect range format");
        }

        Number min = minBoundIndex < 2 ? firstNumber : secondNumber;
        Number max = maxBoundIndex < 2 ? firstNumber : secondNumber;
        return new RangeWithBounds(min, max, leftBoundType, rightBoundType);
    }

    private RangeWithBounds bindBinary(IBoundNode[] children, String type) {
        Number val = (Number) ((LiteralBoundNode) children[0]).getValue();
        Number val2 = (Number) ((LiteralBoundNode) children[1]).getValue();

        if (val.doubleValue() > val2.doubleValue()) {
            throw new OpenLRuntimeException(
                String.format("%s must be more or equal than %s", val2.toString(), val.toString()));
        }

        if (type.endsWith("minus") || type.endsWith("ddot")) {
            return new RangeWithBounds(val, val2);
        }

        if (type.endsWith("tdot")) {
            return new RangeWithBounds(val, val2, BoundType.EXCLUDING, BoundType.EXCLUDING);
        }
        return null;
    }

    private RangeWithBounds bindNumber(IBoundNode[] children) {
        Number val = (Number) ((LiteralBoundNode) children[0]).getValue();
        return new RangeWithBounds(val, val);
    }

    private RangeWithBounds bindPrefix(IBoundNode[] children, String type) {
        Number val = (Number) ((LiteralBoundNode) children[0]).getValue();

        if (type.endsWith("lt")) {
            return new RangeWithBounds(getMin(val), val, BoundType.INCLUDING, BoundType.EXCLUDING);

        } else if (type.endsWith("le")) {
            return new RangeWithBounds(getMin(val), val, BoundType.INCLUDING, BoundType.INCLUDING);

        } else if (type.endsWith("gt")) {
            return new RangeWithBounds(val, getMax(val), BoundType.EXCLUDING, BoundType.INCLUDING);

        } else if (type.endsWith("ge")) {
            return new RangeWithBounds(val, getMax(val), BoundType.INCLUDING, BoundType.INCLUDING);
        }
        return null;
    }

    private RangeWithBounds bindSuffix(IBoundNode[] children, String type) {
        Number val = (Number) ((LiteralBoundNode) children[0]).getValue();

        if (type.endsWith("plus") || type.endsWith("and.more")) {
            return new RangeWithBounds(val, getMax(val), BoundType.INCLUDING, BoundType.INCLUDING);

        } else if (type.endsWith("or.less")) {
            return new RangeWithBounds(getMin(val), val, BoundType.INCLUDING, BoundType.INCLUDING);
        }
        return null;
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
}
