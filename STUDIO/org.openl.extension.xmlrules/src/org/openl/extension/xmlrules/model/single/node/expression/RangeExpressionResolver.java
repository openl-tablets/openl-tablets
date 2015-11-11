package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.RangeNode;

public class RangeExpressionResolver implements ExpressionResolver {
    @Override
    public String resolve(Node leftNode, Node rightNode, Operator operator) {
        if (!(leftNode instanceof RangeNode && rightNode instanceof RangeNode)) {
            return "Error: operator ':' is supported for RangeNode only";
        }

        RangeNode left = (RangeNode) leftNode;
        RangeNode right = (RangeNode) rightNode;

        ExpressionContext context = ExpressionContext.getInstance();
        if (context.isArrayExpression()) {
            int rowShift = context.getCurrentRow() - context.getStartRow();
            int columnShift = context.getCurrentColumn() - context.getStartColumn();

            RangeNode copy = new RangeNode(left);
            copy.setRow("" + (copy.getRowNumber() + rowShift));
            copy.setColumn("" + (copy.getColumnNumber() + columnShift));

            if (right.getRowNumber() < copy.getRowNumber() || right.getColumnNumber() < copy.getColumnNumber()) {
                return "null";
            } else {
                return copy.toOpenLString();
            }
        } else {
            int row = context.getCurrentRow();
            int column = context.getCurrentColumn();

            if (left.getRowNumber() <= row && row <= right.getRowNumber()) {
                RangeNode copy = new RangeNode(left);
                copy.setRow("" + row);
                return copy.toOpenLString();
            } else if (left.getColumnNumber() <= column && column <= right.getColumnNumber()) {
                RangeNode copy = new RangeNode(left);
                copy.setColumn("" + column);
                return copy.toOpenLString();
            } else {
                return "null";
            }
        }
    }
}
