package org.openl.rules.constants;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.types.IUriMember;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class ConstantOpenField extends AOpenField implements IUriMember {

    private ModuleOpenClass declaringClass;
    private Object value;
    private String valueAsString;
    private IMemberMetaInfo memberMetaInfo;
    private String uri;

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
        this.uri = memberMetaInfo.getSourceUrl();
    }

    public String getValueAsString() {
        return valueAsString;
    }
    
    @Override
    public String getUri() {
        return uri;
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