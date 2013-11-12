/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IMemberMetaInfo;

/**
 * @author snshor
 *
 */
public abstract class AOpenField implements IOpenField {
    private String name;
    private IOpenClass type;

    protected AOpenField(String name, IOpenClass type) {
        this.name = name;
        this.type = type;
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

    public String getName() {
        return name;
    }

    public IOpenClass getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isConst()
     */
    public boolean isConst() {
        return false;
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
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#isWritable()
     */
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
