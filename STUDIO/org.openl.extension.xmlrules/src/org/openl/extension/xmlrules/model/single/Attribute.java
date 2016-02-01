package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "attribute")
public class Attribute {
    private String name;
    private AttributeType type;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
