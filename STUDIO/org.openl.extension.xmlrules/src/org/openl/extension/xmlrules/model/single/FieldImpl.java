package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Field;

@XmlType(name = "field")
public class FieldImpl implements Field {
    private String typeName;
    private String name;

    @Override
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
