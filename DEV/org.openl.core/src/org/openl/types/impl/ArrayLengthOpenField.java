/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public abstract class ArrayLengthOpenField implements IOpenField {
    public Object get(Object target, IRuntimeEnv env) {
        return getLength(target);
    }

    public IOpenClass getDeclaringClass() {
        return NullOpenClass.the;
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public abstract int getLength(Object target);

    public String getName() {
        return "length";
    }

    public IOpenClass getType() {
        return JavaOpenClass.INT;
    }

    public boolean isConst() {
        return true;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isStatic() {
        return false;
    }

    public boolean isWritable() {
        return false;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getName();
    }

}
