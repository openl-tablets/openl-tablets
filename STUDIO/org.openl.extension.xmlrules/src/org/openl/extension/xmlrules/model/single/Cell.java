package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;

import org.openl.extension.xmlrules.model.single.node.Node;

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

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
