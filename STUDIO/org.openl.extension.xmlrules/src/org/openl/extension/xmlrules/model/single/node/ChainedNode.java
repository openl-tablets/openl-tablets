package org.openl.extension.xmlrules.model.single.node;

import java.util.Deque;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

public abstract class ChainedNode extends Node {
    private Node node;

    protected void pushToChain(Deque<ChainedNode> nodes) {
        nodes.push(this);
        if (node instanceof ChainedNode) {
            ((ChainedNode) node).pushToChain(nodes);
        }
    }

    @XmlElements({
            @XmlElement(name = "string-node", type=StringNode.class, required = true),
            @XmlElement(name = "number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "if-node", type=IfNode.class, required = true),
            @XmlElement(name = "field-node", type=FieldNode.class, required = true),
            @XmlElement(name = "filter-node", type=FilterNode.class, required = true),
            @XmlElement(name = "parent-node", type=ParentNode.class, required = true)
    })
    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        node.configure(currentWorkbook, currentSheet);
    }
}
