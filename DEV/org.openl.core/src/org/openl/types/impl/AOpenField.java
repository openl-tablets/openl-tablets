/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class AOpenField implements IOpenField {
    private String name;

    private IOpenClass type = JavaOpenClass.OBJECT;

    protected AOpenField(String name, IOpenClass type) {
        this.name = name;
        if (type != null) {
            this.type = type;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return null;
    }

    @Override
    public String getDisplayName(int mode) {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isConst()
     */
    @Override
    public boolean isConst() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isReadable()
     */
    @Override
    public boolean isReadable() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    @Override
    public boolean isStatic() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isWritable()
     */
    @Override
    public boolean isWritable() {
        return false;
    }

    public void setType(IOpenClass class1) {
        type = class1;
    }

    @Override
    public String toString() {
        return name;
    }
}
