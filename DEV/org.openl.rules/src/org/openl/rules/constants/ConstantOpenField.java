package org.openl.rules.constants;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class ConstantOpenField extends AOpenField {

    private final ModuleOpenClass declaringClass;
    private final Object value;
    private final String valueAsString;
    private IMemberMetaInfo memberMetaInfo;

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

    public String getValueAsString() {
        return valueAsString;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    public IMemberMetaInfo getMemberMetaInfo() {
        return memberMetaInfo;
    }

    public void setMemberMetaInfo(IMemberMetaInfo memberMetaInfo) {
        this.memberMetaInfo = memberMetaInfo;
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
