package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "boolean-node")
public class BooleanNode extends Node implements ValueHolder {
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
        return value == null ? null : value;
    }

    @Override
    public String asString() {
        return toOpenLString();
    }
}
