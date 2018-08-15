package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class CompositeMethodWrapper extends CompositeMethod implements IOpenMethodWrapper {
    CompositeMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;

    public CompositeMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, CompositeMethod delegate) {
        super(null, null);
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
    public String toString() {
        return delegate.toString();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IOpenClass getBodyType() {
        return delegate.getBodyType();
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    @Override
    public IBoundMethodNode getMethodBodyBoundNode() {
        return delegate.getMethodBodyBoundNode();
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public boolean isInvokable() {
        return delegate.isInvokable();
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
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
    public void removeDebugInformation() {
        delegate.removeDebugInformation();
    }

    @Override
    public void setMethodBodyBoundNode(IBoundMethodNode node) {
        delegate.setMethodBodyBoundNode(node);
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        delegate.updateDependency(dependencies);
    }

    @Override
    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public ISyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);
    
    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

}
