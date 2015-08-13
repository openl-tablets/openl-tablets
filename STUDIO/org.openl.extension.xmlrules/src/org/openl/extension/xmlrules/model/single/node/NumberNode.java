package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "number-node")
public class NumberNode extends Node implements ValueHolder {

    private Double value;

    @XmlElement(required = true)
    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public String toOpenLString() {
        return value == null ? null : value.toString();
    }
}
