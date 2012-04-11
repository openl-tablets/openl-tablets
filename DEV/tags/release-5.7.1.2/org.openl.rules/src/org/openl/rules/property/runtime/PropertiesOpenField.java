package org.openl.rules.property.runtime;

import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IDynamicObject;
import org.openl.types.impl.AOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertiesOpenField extends AOpenField {

    private TableProperties propertiesInstance;

    public PropertiesOpenField(String name, TableProperties propertiesInstance) {
        super(name, JavaOpenClass.getOpenClass(propertiesInstance.getClass()));
        this.propertiesInstance = propertiesInstance;
    }

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

    public void set(Object target, Object value, IRuntimeEnv env) {
        ((IDynamicObject) target).setFieldValue(getName(), value);
    }
}