/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DynamicObjectField extends AOpenField {

    private IOpenClass declaringClass;

    public DynamicObjectField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(name, type);
        this.declaringClass = declaringClass;
    }

    public DynamicObjectField(String name, IOpenClass type) {
        super(name, type);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        // return ((IDynamicObject)env.getThis()).getFieldValue(name);

        Object res = ((IDynamicObject) target).getFieldValue(getName());

        return res != null ? res : getType().nullObject();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        ((IDynamicObject) target).setFieldValue(getName(), value);
    }
    
    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }
}
