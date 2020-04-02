/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public abstract class ArrayLengthOpenField implements IOpenField {
    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }
        return getLength(target);
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return NullOpenClass.the;
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    public abstract int getLength(Object target);

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.INT;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getName();
    }

}
