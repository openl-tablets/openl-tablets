package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.*;

public class CellInspector {
    public static NodeSize inspect(Node node, boolean allowArrayOperators) {
        if (node instanceof ArrayNode) {
            String[][] array = ((ArrayNode) node).getArray();
            int width = array.length == 0 ? 0 : array[0].length;
            return new NodeSize(array.length, width);
        } else if (node instanceof FunctionNode) {
            return inspectFunction((FunctionNode) node, allowArrayOperators);
        } else if (node instanceof ExpressionNode) {
            return inspectExpression((ExpressionNode) node, allowArrayOperators);
        } else if (node instanceof FilterNode) {
            FilterNode filterNode = (FilterNode) node;
            return inspectFilter(filterNode);
        } else if (node instanceof NamedRangeNode) {
            RangeNode rangeNode = ((NamedRangeNode) node).getRangeNode();
            return new NodeSize(rangeNode.getRowCount(), rangeNode.getColCount());
        }

        return new NodeSize(1, 1);
    }

    private static NodeSize inspectFunction(FunctionNode node, boolean allowArrayOperators) {
        if ("Out".equals(node.getName())) {
            return inspect(node.getArguments().get(0), allowArrayOperators);
        }
        // TODO Different functions can have different sizes
        return new NodeSize(1, 1);
    }

    private static NodeSize inspectExpression(ExpressionNode expressionNode, boolean allowArrayOperators) {
        Operator operator = Operator.findOperator(expressionNode.getOperator());
        if (operator == null) {
            throw new IllegalArgumentException("Unsupported operator");
        }

        switch (operator) {
            case Range:
                RangeNode leftRange = (RangeNode) expressionNode.getLeftNode();
                RangeNode rightRange = (RangeNode) expressionNode.getRightNode();
                return new NodeSize(rightRange.getRowNumber() - leftRange.getRowNumber() + 1, rightRange.getColumnNumber() - leftRange.getColumnNumber() + 1);
            case Addition:
            case Subtraction:
            case Multiplication:
            case Division:
            case Concatenate:
                if (allowArrayOperators) {
                    return new NodeSize(1, 1);
                }
                return getExpressionNodeSize(expressionNode);
            default:
                return getExpressionNodeSize(expressionNode);
        }
    }

    private static NodeSize getExpressionNodeSize(ExpressionNode expressionNode) {
        NodeSize leftSize = inspect(expressionNode.getLeftNode(), true);
        NodeSize rightSize = inspect(expressionNode.getRightNode(), true);
        return new NodeSize(Math.max(leftSize.getResultHeight(), rightSize.getResultHeight()), Math.max(leftSize.getResultWidth(), rightSize.getResultWidth()));
    }

    private static NodeSize inspectFilter(FilterNode filterNode) {
        NodeSize nodeSize = inspect(filterNode.getNode(), false);
        NodeSize conditionSize = inspect(filterNode.getConditionValue(), false);
        return new NodeSize(Math.max(nodeSize.getResultHeight(), conditionSize.getResultHeight()), Math.max(nodeSize.getResultWidth(), conditionSize.getResultWidth()));
    }

    public static class NodeSize {
        private final int resultHeight;
        private final int resultWidth;

        public NodeSize(int resultHeight, int resultWidth) {
            this.resultHeight = resultHeight;
            this.resultWidth = resultWidth;
        }

        public int getResultHeight() {
            return resultHeight;
        }

        public int getResultWidth() {
            return resultWidth;
        }
    }
}
