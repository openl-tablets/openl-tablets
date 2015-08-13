package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "expression-node")
public class ExpressionNode extends Node {
    private Node leftNode;
    private String operator;
    private Node rightNode;

    @XmlElements({
            @XmlElement(name = "left-string-node", type=StringNode.class, required = true),
            @XmlElement(name = "left-number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "left-boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "left-range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "left-expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "left-function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "left-if-node", type=IfNode.class, required = true)
    })
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

    @XmlElements({
            @XmlElement(name = "right-string-node", type=StringNode.class, required = true),
            @XmlElement(name = "right-number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "right-boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "right-range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "right-expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "right-function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "right-if-node", type=IfNode.class, required = true)
    })
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
        return leftNode.toOpenLString() + " " + operator + " " + rightNode.toOpenLString();
    }
}
