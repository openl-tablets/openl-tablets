package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "if-node")
public class IfNode extends Node {
    private Node condition;
    private Node thenNode;
    private Node elseNode;

    @XmlElements({
            @XmlElement(name = "condition-string-node", type=StringNode.class, required = true),
            @XmlElement(name = "condition-number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "condition-boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "condition-range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "condition-expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "condition-function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "condition-if-node", type=IfNode.class, required = true)
    })
    public Node getCondition() {
        return condition;
    }

    public void setCondition(Node condition) {
        this.condition = condition;
    }

    @XmlElements({
            @XmlElement(name = "then-string-node", type=StringNode.class, required = true),
            @XmlElement(name = "then-number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "then-boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "then-range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "then-expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "then-function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "then-if-node", type=IfNode.class, required = true)
    })
    public Node getThenNode() {
        return thenNode;
    }

    public void setThenNode(Node thenNode) {
        this.thenNode = thenNode;
    }

    @XmlElements({
            @XmlElement(name = "else-string-node", type=StringNode.class, required = true),
            @XmlElement(name = "else-number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "else-boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "else-range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "else-expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "else-function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "else-if-node", type=IfNode.class, required = true)
    })
    public Node getElseNode() {
        return elseNode;
    }

    public void setElseNode(Node elseNode) {
        this.elseNode = elseNode;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        condition.configure(currentWorkbook, currentSheet);
        thenNode.configure(currentWorkbook, currentSheet);
        elseNode.configure(currentWorkbook, currentSheet);
    }

    @Override
    public String toOpenLString() {
        return "(" + condition.toOpenLString() + "?" + thenNode.toOpenLString() + ":" + elseNode.toOpenLString();
    }
}
