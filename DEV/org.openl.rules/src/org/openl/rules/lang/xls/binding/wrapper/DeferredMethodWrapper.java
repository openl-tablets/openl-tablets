package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.module.DeferredMethod;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class DeferredMethodWrapper extends DeferredMethod implements IOpenMethodWrapper {
    DeferredMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;

    public DeferredMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, DeferredMethod delegate) {
        super(null, null, null, null, null);
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(this, target, params, env);
    }

    @Override
    public XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClass;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public ISyntaxNode getMethodBodyNode() {
        return delegate.getMethodBodyNode();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public void setMethodBodyBoundNode(IBoundMethodNode bnode) {
        delegate.setMethodBodyBoundNode(bnode);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache();

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

}
