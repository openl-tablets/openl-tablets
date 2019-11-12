package org.openl.rules.lang.xls.binding.wrapper;

import java.util.List;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class OverloadedMethodsDispatcherTableWrapper extends OverloadedMethodsDispatcherTable implements IOpenMethodWrapper {
    OverloadedMethodsDispatcherTable delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    ContextPropertiesInjector contextPropertiesInjector;

    public OverloadedMethodsDispatcherTableWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            OverloadedMethodsDispatcherTable delegate,
            ContextPropertiesInjector contextPropertiesInjector) {
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.contextPropertiesInjector = contextPropertiesInjector;
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
    public IOpenMethod getDecisionTableOpenMethod() {
        return delegate.getDecisionTableOpenMethod();
    }

    @Override
    public void setDecisionTableOpenMethod(IOpenMethod decisionTableOpenMethod) {
        delegate.setDecisionTableOpenMethod(decisionTableOpenMethod);
    }

    @Override
    public TableSyntaxNode getDispatcherTable() {
        return delegate.getDispatcherTable();
    }

    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public void addMethod(IOpenMethod candidate) {
        delegate.addMethod(candidate);
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
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public IOpenMethod getTargetMethod() {
        return delegate.getTargetMethod();
    }

    @Override
    public List<IOpenMethod> getCandidates() {
        return delegate.getCandidates();
    }

    @Override
    public boolean isConstructor() {
        return delegate.isConstructor();
    }

    @Override
    public IOpenMethod findMatchingMethod(IRuntimeEnv env) {
        IOpenMethod openMethod = WrapperLogic.getTopClassMethod(this, env);
        return ((OpenMethodDispatcher) openMethod).findMatchingMethod(env);
    }

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

    @Override
    public ContextPropertiesInjector getContextPropertiesInjector() {
        return contextPropertiesInjector;
    }

}
