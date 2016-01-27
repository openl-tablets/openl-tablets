/*
 * Created on Jun 23, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.types.impl;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
 */
public abstract class AMethod implements IOpenMethod, IModuleInfo {

    private IOpenMethodHeader header;
    
    private String dependencyName;
    
    public final String getModuleName() {
        return dependencyName;
    }
    
    public final void setModuleName(String dependencyName) {
        this.dependencyName = dependencyName;
    }

    public AMethod(IOpenMethodHeader header) {
        this.header = header;
    }

    public IOpenClass getDeclaringClass() {
        return header.getDeclaringClass();
    }

    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public IMemberMetaInfo getInfo() {
        return header.getInfo();
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public String getName() {
        return header.getName();
    }

    public IMethodSignature getSignature() {
        return header.getSignature();
    }

    public IOpenClass getType() {
        return header.getType();
    }

    public boolean isStatic() {
        return header.isStatic();
    }

}
