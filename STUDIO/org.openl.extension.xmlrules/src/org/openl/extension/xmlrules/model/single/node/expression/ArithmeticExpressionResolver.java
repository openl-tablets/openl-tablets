package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.*;

public class ArithmeticExpressionResolver extends SimpleExpressionResolver {
    @Override
    public String resolve(Node leftNode, Node rightNode, Operator operator) {
        return addCastIfNeeded(leftNode) + " " + operator.getOpenLOperator() + " " + addCastIfNeeded(rightNode);
    }

    private String addCastIfNeeded(Node node) {
        if (node == null) {
            return "";
        }

        if (node instanceof NumberNode) {
            return toString(node);
        }

        if (node instanceof ArrayNode) {
            return toString(node);
        }

        if (node instanceof ExpressionNode) {
            ExpressionNode expressionNode = (ExpressionNode) node;
            ExpressionResolver expressionResolver = ExpressionResolverFactory.getExpressionResolver(expressionNode.getOperator());

            if (expressionResolver instanceof RangeExpressionResolver && ((RangeExpressionResolver) expressionResolver).isRangeReturnsArray()) {
                return toString(node);
            }
        }

        if (node instanceof RangeNode && ((RangeNode) node).getHasArrayFormula()) {
            return toString(node);
        }

        if (node instanceof NamedRangeNode) {
            RangeNode rangeNode = ((NamedRangeNode) node).getRangeNode();
            if (rangeNode.getColumnNumber() > 1 || rangeNode.getRowCount() > 1) {
                return toString(node);
            }
        }

        return "((Double) (" + node.toOpenLString() + "))";
    }
}
