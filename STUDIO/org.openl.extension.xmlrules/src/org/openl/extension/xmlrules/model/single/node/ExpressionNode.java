package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

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
        // TODO Support fixed number of Excel operators
        String leftCast = leftNode instanceof RangeNode ? "(String) " : "";
        String rightCast = rightNode instanceof RangeNode ? " (String) " : "";
        return leftCast + leftNode.toOpenLString() + " " + operator + rightCast + rightNode.toOpenLString();
    }
}
