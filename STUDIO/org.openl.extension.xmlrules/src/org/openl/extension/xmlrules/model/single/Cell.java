package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import org.openl.extension.xmlrules.model.single.node.*;

public class Cell {
    private String address;
    private Node node;

    @XmlElement(required = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @XmlElements({
            @XmlElement(name = "string-node", type=StringNode.class, required = true),
            @XmlElement(name = "number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "if-node", type=IfNode.class, required = true),
            @XmlElement(name = "filter-node", type=FilterNode.class, required = true)
    })
    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
