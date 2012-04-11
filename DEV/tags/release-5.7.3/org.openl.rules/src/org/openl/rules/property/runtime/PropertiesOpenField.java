package org.openl.rules.property.runtime;

import java.util.Map;

import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IDynamicObject;
import org.openl.types.impl.AOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertiesOpenField extends AOpenField {

    private Map<String, Object> properties;

    public PropertiesOpenField(String name, TableProperties propertiesInstance) {
        super(name, JavaOpenClass.getOpenClass(propertiesInstance.getClass()));
        this.properties = propertiesInstance.getAllProperties();
    }

    public Object get(Object target, IRuntimeEnv env) {
        Object data = ((IDynamicObject) target).getFieldValue(getName());

        if (data == null) {
            data = properties;
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