package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Field;

@XmlType(name = "field")
public class FieldImpl implements Field {
    private String typeName;
    private Boolean isArray = Boolean.FALSE;
    private String name;

    @XmlElement(name = "type-name")
    @Override
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @XmlElement(name = "is-array", defaultValue = "false")
    @Override
    public Boolean getIsArray() {
        return isArray;
    }

    public void setIsArray(Boolean isArray) {
        this.isArray = isArray;
    }

    @XmlElement(required = true)
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
