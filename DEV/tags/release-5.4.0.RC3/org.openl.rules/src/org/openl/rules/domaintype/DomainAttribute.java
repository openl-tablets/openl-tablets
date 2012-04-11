package org.openl.rules.domaintype;

import org.openl.meta.StringValue;
import org.openl.types.IOpenClass;

public class DomainAttribute {

    IOpenClass base;
    StringValue name;
    StringValue newType;

    public IOpenClass getBase() {
        return base;
    }

    public StringValue getName() {
        return name;
    }

    public StringValue getNewType() {
        return newType;
    }

    public void setBase(IOpenClass base) {
        this.base = base;
    }

    public void setName(StringValue name) {
        this.name = name;
    }

    public void setNewType(StringValue newType) {
        this.newType = newType;
    }

}
