/*
 * Created on Jul 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
 *
 */
public class OpenMethodHeader implements IOpenMethodHeader {

    protected IMethodSignature signature;
    protected IOpenClass declaringClass;
    protected IOpenClass typeClass;

    protected String name;
    protected boolean isStatic;

    protected IMemberMetaInfo info;

    public OpenMethodHeader(String name, IOpenClass typeClass, IMethodSignature signature, IOpenClass declaringClass) {
        this(name, typeClass, signature, declaringClass, false, null);
    }

    /**
     *
     */
    public OpenMethodHeader(String name, IOpenClass typeClass, IMethodSignature signature, IOpenClass declaringClass,
            boolean isStatic, IMemberMetaInfo info) {
        this.name = name;
        this.typeClass = typeClass;
        this.signature = signature;
        this.declaringClass = declaringClass;
        this.isStatic = isStatic;
        this.info = info;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    public String getDisplayName(int mode) {
        return MethodUtil.printMethod(this, mode, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    public IMemberMetaInfo getInfo() {
        return info;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public IMethodSignature getSignature() {
        return signature;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    public IOpenClass getType() {
        return typeClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    public boolean isStatic() {
        return isStatic;
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    public void setTypeClass(IOpenClass typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        buf.append(getType().getName()).append(' ');
        MethodUtil.printMethod(getName(), getSignature(), buf);
        return buf.toString();
    }

}
