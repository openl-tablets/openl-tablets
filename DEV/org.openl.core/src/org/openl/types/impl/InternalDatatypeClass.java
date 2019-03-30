package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

/**
 * Handles internal type that is defined through datatype mechanism.
 * 
 * @author DLiauchuk
 *
 */
public class InternalDatatypeClass implements IOpenMember {

    private IOpenClass domainOpenClass;
    private IOpenClass declaringClass;

    public InternalDatatypeClass(IOpenClass domenOpenClass, IOpenClass declaringClass) {
        this.domainOpenClass = domenOpenClass;
        this.declaringClass = declaringClass;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getType() {
        return domainOpenClass;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public String getDisplayName(int mode) {
        return domainOpenClass.getName();
    }

    @Override
    public String getName() {
        return domainOpenClass.getName();
    }

}
