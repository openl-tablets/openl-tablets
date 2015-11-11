package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.ExpressionNode;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.RangeNode;

public class SimpleExpressionResolver implements ExpressionResolver {
    @Override
    public String resolve(Node leftNode, Node rightNode, Operator operator) {
        return toString(leftNode) + " " + operator.getOpenLOperator() + " " + toString(rightNode);
    }

    protected String toString(Node node) {
        String nodeString;

        if (node != null) {
            nodeString = node.toOpenLString();

            if (node instanceof RangeNode) {
                nodeString = "((String) " + nodeString + ")";
            } else if (node instanceof ExpressionNode) {
                nodeString = "(" + nodeString + ")";
            }
        } else {
            nodeString = "";
        }

        return nodeString;
    }
}
