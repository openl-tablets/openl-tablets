/*
 * Created on Jun 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ConstOpenField implements IOpenField {

    String name;
    Object value;
    IOpenClass type;

    public ConstOpenField(String name, Object value, IOpenClass type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#get(java.lang.Object)
     */
    public Object get(Object target, IRuntimeEnv env) {
        return value;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    public IOpenClass getDeclaringClass() {
        return null;
    }

    public String getDisplayName(int mode) {
        return name;
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
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    public IOpenClass getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isConst()
     */
    public boolean isConst() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isReadable()
     */
    public boolean isReadable() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    public boolean isStatic() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isWritable()
     */
    public boolean isWritable() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#set(java.lang.Object, java.lang.Object)
     */
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getName();
    }

}
