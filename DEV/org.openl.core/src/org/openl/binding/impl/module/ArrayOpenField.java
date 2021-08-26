package org.openl.binding.impl.module;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

import java.lang.reflect.Array;

public class ArrayOpenField implements IOpenField {

    private final IOpenField field;
    private final IOpenClass type;

    public ArrayOpenField(IOpenField field, int dimension){
        this.field = field;
        this.type = field.getType().getArrayType(dimension);
    }

    @Override
    public String getDisplayName(int mode) {
        return field.getDisplayName(mode);
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return processArray(target, env, getType());
    }

    private Object processArray(Object target, IRuntimeEnv env, IOpenClass type) {
        int length = Array.getLength(target);
        Object arrayResult = Array.newInstance(type.getComponentClass().getInstanceClass(), length);
        for (int i = 0; i < length; i++) {
            Object arrayElement = Array.get(target, i);
            if (type.getComponentClass().isArray()) {
                arrayElement = processArray(arrayElement, env, type.getComponentClass());
                Array.set(arrayResult, i, arrayElement);
            } else {
                Array.set(arrayResult, i, field.get(arrayElement, env));
            }
        }
        return arrayResult;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        field.set(target, value, env);
    }

    @Override
    public boolean isConst() {
        return field.isConst();
    }

    @Override
    public boolean isReadable() {
        return field.isReadable();
    }

    @Override
    public boolean isWritable() {
        return field.isWritable();
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isStatic() {
        return field.isStatic();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return field.getInfo();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return field.getDeclaringClass();
    }
}
