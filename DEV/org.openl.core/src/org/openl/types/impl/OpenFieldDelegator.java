/*
 * Created on Jul 1, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class OpenFieldDelegator implements IOpenField {
    protected IOpenField field;

    public OpenFieldDelegator(IOpenField field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        if (obj instanceof OpenFieldDelegator) {
            OpenFieldDelegator d = (OpenFieldDelegator) obj;
            return field.equals(d.field);
        }

        if (obj instanceof IOpenField) {
            IOpenField f = (IOpenField) obj;
            return field.equals(f);
        }

        return super.equals(obj);
    }

    /**
     * @param target
     * @return
     */
    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return field.get(target, env);
    }

    /**
     * @return
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return field.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return field.getDisplayName(mode);
    }

    public IOpenField getField() {
        return field;
    }

    /**
     * @return
     */
    @Override
    public IMemberMetaInfo getInfo() {
        return field.getInfo();
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return field.getName();
    }

    /**
     * @return
     */
    @Override
    public IOpenClass getType() {
        return field.getType();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return field.hashCode();
    }

    /**
     * @return
     */
    @Override
    public boolean isConst() {
        return field.isConst();
    }

    /**
     * @return
     */
    @Override
    public boolean isReadable() {
        return field.isReadable();
    }

    /**
     * @return
     */
    @Override
    public boolean isStatic() {
        return field.isStatic();
    }

    /**
     * @return
     */
    @Override
    public boolean isWritable() {
        return field.isWritable();
    }

    /**
     * @param target
     * @param value
     */
    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        field.set(target, value, env);
    }

    @Override
    public String toString() {
        return field.toString();
    }

}
