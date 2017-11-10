/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IMemberMetaInfo;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class JavaOpenField implements IOpenField {

    Field field;

    JavaOpenField(Field field) {
        this.field = field;
    }

    public Object get(Object target, IRuntimeEnv env) {
        try {
            return field.get(target);
        } catch (Exception t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    public IOpenClass getDeclaringClass() {
        return JavaOpenClass.getOpenClass(field.getDeclaringClass());
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    public IMemberMetaInfo getInfo() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return field.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(field.getType());
    }

    public boolean isConst() {
        return Modifier.isFinal(field.getModifiers());
    }

    public boolean isReadable() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    public boolean isStatic() {
        return Modifier.isStatic(field.getModifiers());
    }

    public boolean isWritable() {
        return true;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        try {
            field.set(target, value);
        } catch (Exception t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public Field getJavaField() {
        return field;
    }
}
