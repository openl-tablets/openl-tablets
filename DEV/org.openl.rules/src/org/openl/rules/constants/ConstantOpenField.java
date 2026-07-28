package org.openl.rules.constants;

import lombok.Getter;
import lombok.Setter;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class ConstantOpenField extends AOpenField {

    private ModuleOpenClass declaringClass;
    @Getter
    private Object value;
    @Getter
    private String valueAsString;
    @Getter
    @Setter
    private IMemberMetaInfo memberMetaInfo;

    public ConstantOpenField() {
        super(null, null);
    }

    public ConstantOpenField(String name,
                             Object value,
                             String valueAsString,
                             IOpenClass type,
                             ModuleOpenClass declaringClass,
                             IMemberMetaInfo memberMetaInfo) {
        super(name, type);
        this.declaringClass = declaringClass;
        this.value = value;
        this.valueAsString = valueAsString;
        this.memberMetaInfo = memberMetaInfo;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return value;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }
}
