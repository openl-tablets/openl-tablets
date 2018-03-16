package org.openl.rules.constants;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.types.TableUriMember;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class ConstantOpenField extends AOpenField implements TableUriMember{

    private ModuleOpenClass declaringClass;
    private Object value;
    private String tableUri;

    public ConstantOpenField(String name, Object value, IOpenClass type, ModuleOpenClass declaringClass, String tableUri) {
        super(name, type);
        this.declaringClass = declaringClass;
        this.value = value;
        this.tableUri = tableUri;
    }
    
    public String getTableUri() {
        return tableUri;
    }
    
    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

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
    
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }
}