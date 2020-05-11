package org.openl.rules.lang.xls.types;

import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class TransientOpenField extends ADatatypeOpenField {
    public TransientOpenField(IOpenClass declaringClass, String name, IOpenClass type, String contextProperty) {
        super(declaringClass, name, type, contextProperty);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }
        Object res = ((SimpleRulesRuntimeEnv) env).getTransientFieldValue(target, getName());
        return res != null ? res : getType().nullObject();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target != null) {
            ((SimpleRulesRuntimeEnv) env).setTransientFieldValue(target, getName(), value);
        }
    }

}
