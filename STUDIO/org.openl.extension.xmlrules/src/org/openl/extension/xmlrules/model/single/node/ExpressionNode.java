package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.single.node.expression.ExpressionResolver;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionResolverFactory;
import org.openl.extension.xmlrules.model.single.node.expression.Operator;

@XmlType(name = "expression-node")
public class ExpressionNode extends Node {
    private Node leftNode;
    private String operator;
    private Node rightNode;

    public Node getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(Node leftNode) {
        this.leftNode = leftNode;
    }

    @XmlElement(required = true)
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Node getRightNode() {
        return rightNode;
    }

    public void setRightNode(Node rightNode) {
        this.rightNode = rightNode;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        leftNode.configure(currentWorkbook, currentSheet);
        rightNode.configure(currentWorkbook, currentSheet);
    }

    @Override
    public String toOpenLString() {
        Operator op = Operator.findOperator(operator);
        if (op == null) {
            throw new UnsupportedOperationException("Operator " + operator + " isn't supported");
        }
        ExpressionResolver resolver = ExpressionResolverFactory.getExpressionResolver(op);
        return resolver.resolve(leftNode, rightNode, op);
    }
}
