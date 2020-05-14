package org.openl.rules.lang.xls.types;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class TransientOpenField extends ADatatypeOpenField {
    private final TransientFieldsValues transientFieldsValues = new TransientFieldsValues();

    public TransientOpenField(IOpenClass declaringClass, String name, IOpenClass type, String contextProperty) {
        super(declaringClass, name, type, contextProperty);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }
        Object res = transientFieldsValues.getValue(target);
        return res != null ? res : getType().nullObject();
    }

    public TransientFieldsValues getTransientFieldsValues() {
        return transientFieldsValues;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target != null) {
            transientFieldsValues.setValue(target, value);
        }
    }

}
