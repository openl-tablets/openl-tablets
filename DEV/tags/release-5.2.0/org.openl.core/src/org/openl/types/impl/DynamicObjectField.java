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

    IOpenClass declaringClass;

    /**
     * @param name
     * @param type
     */
    public DynamicObjectField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(name, type);
        this.declaringClass = declaringClass;
    }

    public DynamicObjectField(String name, IOpenClass type) {
        super(name, type);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#get(java.lang.Object)
     */
    public Object get(Object target, IRuntimeEnv env) {
        // return ((IDynamicObject)env.getThis()).getFieldValue(name);

        Object res = ((IDynamicObject) target).getFieldValue(name);

        return res != null ? res : getType().nullObject();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    /**
     *
     */

    @Override
    public boolean isWritable() {
        // TODO check final attribute
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#set(java.lang.Object, java.lang.Object)
     */
    public void set(Object target, Object value, IRuntimeEnv env) {
        ((IDynamicObject) target).setFieldValue(name, value);
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

}
