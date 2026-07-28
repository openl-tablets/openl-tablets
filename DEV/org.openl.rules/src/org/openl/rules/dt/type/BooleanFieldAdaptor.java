package org.openl.rules.dt.type;

import lombok.RequiredArgsConstructor;

import org.openl.types.IOpenField;

@RequiredArgsConstructor
public class BooleanFieldAdaptor extends BooleanTypeAdaptor {

    private final IOpenField field;

    @Override
    public boolean extractBooleanValue(Object target) {
        return (Boolean) field.get(target, null);
    }

}
