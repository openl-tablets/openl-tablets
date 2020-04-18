package org.openl.rules.property;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertiesOpenField extends AOpenField {

    private TableProperties propertiesInstance;
    private final ModuleOpenClass declaringClass;

    public PropertiesOpenField(String name, TableProperties propertiesInstance, ModuleOpenClass declaringClass) {
        super(name, JavaOpenClass.getOpenClass(propertiesInstance.getClass()));
        this.propertiesInstance = propertiesInstance;
        this.declaringClass = declaringClass;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        Object data = ((IDynamicObject) target).getFieldValue(getName());

        if (data == null) {
            data = propertiesInstance;
            ((IDynamicObject) target).setFieldValue(getName(), data);
        }

        return data;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    public TableProperties getPropertiesInstance() {
        return propertiesInstance;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        ((IDynamicObject) target).setFieldValue(getName(), value);
    }

    /* package */ void setPropertiesInstance(TableProperties propertiesInstance) {
        this.propertiesInstance = propertiesInstance;
    }
}