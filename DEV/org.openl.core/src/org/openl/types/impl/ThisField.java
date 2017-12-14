package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ThisField extends AOpenField {
    public ThisField(IOpenClass type) {
        super("this", type);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return target;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException("Can not assign to 'this'");

    }
}
