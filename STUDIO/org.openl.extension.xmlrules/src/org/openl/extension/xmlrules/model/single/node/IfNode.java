package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.single.Cell;

@XmlType(name = "if-node")
public class IfNode extends Node {
    private Node condition;
    private Node thenNode;
    private Node elseNode;

    public Node getCondition() {
        return condition;
    }

    public void setCondition(Node condition) {
        this.condition = condition;
    }

    public Node getThenNode() {
        return thenNode;
    }

    public void setThenNode(Node thenNode) {
        this.thenNode = thenNode;
    }

    public Node getElseNode() {
        return elseNode;
    }

    public void setElseNode(Node elseNode) {
        this.elseNode = elseNode;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        condition.configure(currentWorkbook, currentSheet, cell);
        thenNode.configure(currentWorkbook, currentSheet, cell);
        elseNode.configure(currentWorkbook, currentSheet, cell);
    }

    @Override
    public String toOpenLString() {
        String conditionCast = condition instanceof RangeNode ? "(Boolean) " : "";
        return "(" + conditionCast + condition.toOpenLString() + ") ? " + thenNode.toOpenLString() + ":" + elseNode.toOpenLString();
    }
}
