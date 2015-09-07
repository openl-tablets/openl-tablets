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

public class CompositeMethodWrapper extends CompositeMethod implements IOpenMethodWrapper{
    CompositeMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public CompositeMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, CompositeMethod delegate) {
        super(null, null);
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(xlsModuleOpenClass, this, target, params, env);
    }

    public String toString() {
        return delegate.toString();
    }

    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public IOpenClass getBodyType() {
        return delegate.getBodyType();
    }

    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    public IBoundMethodNode getMethodBodyBoundNode() {
        return delegate.getMethodBodyBoundNode();
    }
    
    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public boolean isInvokable() {
        return delegate.isInvokable();
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public String getName() {
        return delegate.getName();
    }

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public void removeDebugInformation() {
        delegate.removeDebugInformation();
    }

    public void setMethodBodyBoundNode(IBoundMethodNode node) {
        delegate.setMethodBodyBoundNode(node);
    }

    public void updateDependency(BindingDependencies dependencies) {
        delegate.updateDependency(dependencies);
    }

    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public ISyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

}
