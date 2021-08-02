package org.openl.dependency;

import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class ModuleVar extends AOpenField {
    public ModuleVar(String name, IOpenClass type) {
        super(name, type);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return target;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return super.getType();
    }
}
