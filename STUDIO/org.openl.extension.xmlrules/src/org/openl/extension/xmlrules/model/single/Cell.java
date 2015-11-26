package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

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

    @XmlTransient()
    public Boolean getHasArrayFormula() {
        return getAddress().getHasArrayFormula();
    }

    @XmlTransient()
    public RangeNode getEndAddress() {
        if (!getHasArrayFormula()) {
            return null;
        }

        RangeNode endAddress = new RangeNode();
        endAddress.setPath(address.getPath());
        endAddress.setRow("" + (address.getRowNumber() + address.getRowCount() - 1));
        endAddress.setColumn("" + (address.getColumnNumber() + address.getColCount() - 1));
        return endAddress;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
