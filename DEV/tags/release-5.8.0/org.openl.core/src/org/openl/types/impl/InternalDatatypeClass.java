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
public class InternalDatatypeClass implements IOpenMember{    
    
    private IOpenClass domainOpenClass;
    private IOpenClass declaringClass;
    
    public InternalDatatypeClass(IOpenClass domenOpenClass, IOpenClass declaringClass) {
        this.domainOpenClass = domenOpenClass;
        this.declaringClass = declaringClass;
    }
    
    public IOpenClass getDeclaringClass() {        
        return declaringClass;
    }

    public IMemberMetaInfo getInfo() {        
        return null;
    }

    public IOpenClass getType() {        
        return domainOpenClass;
    }

    public boolean isStatic() {        
        return false;
    }

    public String getDisplayName(int mode) {        
        return domainOpenClass.getName();
    }

    public String getName() {        
        return domainOpenClass.getName();
    }

}
