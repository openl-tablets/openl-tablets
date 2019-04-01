/*
 * Created on Jun 23, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.types.impl;

import org.openl.binding.MethodUtil;
import org.openl.types.*;

/**
 * @author snshor
 */
public abstract class AMethod implements IOpenMethod, IModuleInfo {

    private IOpenMethodHeader header;

    private String dependencyName;

    @Override
    public String getModuleName() {
        return dependencyName;
    }

    public void setModuleName(String dependencyName) {
        this.dependencyName = dependencyName;
    }

    public AMethod(IOpenMethodHeader header) {
        this.header = header;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return header.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return header.getInfo();
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public String getName() {
        return header.getName();
    }

    @Override
    public IMethodSignature getSignature() {
        return header.getSignature();
    }

    @Override
    public IOpenClass getType() {
        return header.getType();
    }

    @Override
    public boolean isStatic() {
        return header.isStatic();
    }

}
