package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "failure-node")
public class FailureNode extends Node {
    private String value;

    @XmlElement(required = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toOpenLString() {
        throw new IllegalArgumentException("Error in serialization: " + value);
    }
}
