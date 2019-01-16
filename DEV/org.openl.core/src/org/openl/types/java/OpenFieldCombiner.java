package org.openl.types.java;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class OpenFieldCombiner implements IOpenField {

    private final IOpenField origReadField;
    private final IOpenField origWriteField;

    public OpenFieldCombiner(IOpenField origReadField, IOpenField origWriteField) {
        this.origReadField = origReadField;
        this.origWriteField = origWriteField;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return origReadField.get(target, env);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        origWriteField.set(target, value, env);
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return origReadField.isReadable();
    }

    @Override
    public boolean isWritable() {
        return origWriteField.isWritable();
    }

    @Override
    public IOpenClass getType() {
        return origReadField.getType();
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null; // STUB
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return origReadField.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    @Override
    public String getName() {
        return origReadField.getName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
