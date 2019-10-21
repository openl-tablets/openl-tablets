package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ThisField extends AOpenField {
    public static final String THIS = "this";

    public ThisField(IOpenClass type) {
        super(THIS, type);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return target;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException("Cannot assign to 'this'");

    }
}
