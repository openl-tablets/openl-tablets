package org.openl.extension.xmlrules.model.single;

import org.openl.extension.xmlrules.model.Field;

public class FieldImpl implements Field {
    private String typeName;
    private String name;
    private String reference;

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

    @Override
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
