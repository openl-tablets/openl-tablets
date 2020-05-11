package org.openl.rules.lang.xls.types;

import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;

public abstract class ADatatypeOpenField extends AOpenField {

    protected final IOpenClass declaringClass;
    protected final String contextProperty;

    public ADatatypeOpenField(IOpenClass declaringClass, String name, IOpenClass type, String contextProperty) {
        super(name, type);
        this.declaringClass = declaringClass;
        this.contextProperty = contextProperty;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean isContextProperty() {
        return contextProperty != null;
    }

    @Override
    public String getContextProperty() {
        return contextProperty;
    }
}