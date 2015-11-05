package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;

import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.RangeNode;

public class Cell {
    private RangeNode address;
    private Node node;

    @XmlElement(required = true)
    public RangeNode getAddress() {
        return address;
    }

    public void setAddress(RangeNode address) {
        this.address = address;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
