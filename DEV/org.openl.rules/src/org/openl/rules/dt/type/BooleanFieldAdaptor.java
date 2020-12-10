package org.openl.rules.dt.type;

import org.openl.types.IOpenField;

public class BooleanFieldAdaptor extends BooleanTypeAdaptor {

    private final IOpenField field;

    public BooleanFieldAdaptor(IOpenField field) {
        this.field = field;
    }

    @Override
    public boolean extractBooleanValue(Object target) {
        return (Boolean) field.get(target, null);
    }

}
