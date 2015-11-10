package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.NumberNode;

public class AdditionExpressionResolver extends SimpleExpressionResolver {
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

        return "((Double) (" + node.toOpenLString() + "))";
    }
}
