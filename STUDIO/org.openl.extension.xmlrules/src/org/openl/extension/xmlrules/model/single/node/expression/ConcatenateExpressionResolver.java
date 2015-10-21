package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.StringNode;

public class ConcatenateExpressionResolver extends SimpleExpressionResolver {
    @Override
    public String resolve(Node leftNode, Node rightNode, Operator operator) {
        return addCastIfNeeded(leftNode) + " " + operator.getOpenLOperator() + " " + addCastIfNeeded(rightNode);
    }

    private String addCastIfNeeded(Node node) {
        if (node == null) {
            return "";
        }

        if (node instanceof StringNode) {
            return toString(node);
        }

        return "((String)" + toString(node) + ")";
    }
}
