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
    protected final IOpenField delegate;

    public OpenFieldDelegator(IOpenField field) {
        this.delegate = field;
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
            return delegate.equals(d.delegate);
        }

        if (obj instanceof IOpenField) {
            IOpenField f = (IOpenField) obj;
            return delegate.equals(f);
        }

        return super.equals(obj);
    }

    /**
     * @param target
     * @return
     */
    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return delegate.get(target, env);
    }

    /**
     * @return
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public IOpenField getDelegate() {
        return delegate;
    }

    /**
     * @return
     */
    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /**
     * @return
     */
    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * @return
     */
    @Override
    public boolean isConst() {
        return delegate.isConst();
    }

    /**
     * @return
     */
    @Override
    public boolean isReadable() {
        return delegate.isReadable();
    }

    /**
     * @return
     */
    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    /**
     * @return
     */
    @Override
    public boolean isWritable() {
        return delegate.isWritable();
    }

    /**
     * @param target
     * @param value
     */
    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        delegate.set(target, value, env);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
