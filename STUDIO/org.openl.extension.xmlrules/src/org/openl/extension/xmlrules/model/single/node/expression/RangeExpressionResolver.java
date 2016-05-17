package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.XmlRulesPath;
import org.openl.extension.xmlrules.model.single.node.NamedRangeNode;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.RangeNode;
import org.openl.extension.xmlrules.utils.CellReference;

public class RangeExpressionResolver implements ExpressionResolver {
    @Override
    public String resolve(Node leftNode, Node rightNode, Operator operator) {
        if (!((leftNode instanceof RangeNode || leftNode instanceof NamedRangeNode) &&
                (rightNode instanceof RangeNode || rightNode instanceof NamedRangeNode))) {
            return "Error: operator ':' is supported for RangeNode only";
        }

        RangeNode left = leftNode instanceof NamedRangeNode ? ((NamedRangeNode) leftNode).getRangeNode() : (RangeNode) leftNode;
        RangeNode right = rightNode instanceof NamedRangeNode ? ((NamedRangeNode) rightNode).getRangeNode() :  (RangeNode) rightNode;

        ExpressionContext context = ExpressionContext.getInstance();

        if (context.isCanHandleArrayOperators()) {
            CellReference reference = left.getReference();
            XmlRulesPath path = ExpressionContext.getInstance().getCurrentPath();
            int rowCount = right.getRowNumber() - left.getRowNumber() + 1;
            int columnCount = right.getColumnNumber() - left.getColumnNumber() + 1;
            if (!reference.getWorkbook().equals(path.getWorkbook())) {
                return String.format("CellRange(\"%s\", \"%s\", %d, %d, %d, %d)",
                        reference.getWorkbook(),
                        reference.getSheet(),
                        reference.getRowNumber(),
                        reference.getColumnNumber(),
                        rowCount,
                        columnCount);
            } else if (!reference.getSheet().equals(path.getSheet())) {
                return String.format("CellRange(\"%s\", %d, %d, %d, %d)",
                        reference.getSheet(),
                        reference.getRowNumber(),
                        reference.getColumnNumber(),
                        rowCount,
                        columnCount);
            } else {
                return String.format("CellRange(%d, %d, %d, %d)",
                        reference.getRowNumber(),
                        reference.getColumnNumber(),
                        rowCount,
                        columnCount);
            }
        }

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

    public boolean isRangeReturnsArray() {
        ExpressionContext instance = ExpressionContext.getInstance();
        return instance.isCanHandleArrayOperators();
    }
}
