package org.openl.types.impl;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.util.text.ILocation;

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

    protected ILocation typeLocation;
    protected ILocation[] paramTypeLocations;

    public OpenMethodHeader(String name,
            IOpenClass typeClass,
            IMethodSignature signature,
            IOpenClass declaringClass,
            ILocation typeLocation,
            ILocation[] paramTypeLocations) {
        this(name, typeClass, signature, declaringClass);
        this.typeLocation = typeLocation;
        this.paramTypeLocations = paramTypeLocations;
    }

    public OpenMethodHeader(String name, IOpenClass typeClass, IMethodSignature signature, IOpenClass declaringClass) {
        this.name = name;
        this.typeClass = typeClass;
        this.signature = signature;
        this.declaringClass = declaringClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    @Override
    public IMemberMetaInfo getInfo() {
        return info;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public IMethodSignature getSignature() {
        return signature;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    @Override
    public IOpenClass getType() {
        return typeClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    @Override
    public boolean isStatic() {
        return isStatic;
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    public void setTypeClass(IOpenClass typeClass) {
        this.typeClass = typeClass;
    }

    public ILocation getTypeLocation() {
        return typeLocation;
    }

    public ILocation[] getParamTypeLocations() {
        return paramTypeLocations;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append(getType().getName()).append(' ');
        MethodUtil.printMethod(this, buf);
        return buf.toString();
    }

}
