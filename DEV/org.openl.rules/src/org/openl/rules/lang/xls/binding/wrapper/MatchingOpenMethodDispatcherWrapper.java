package org.openl.rules.lang.xls.binding.wrapper;

import java.util.List;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class MatchingOpenMethodDispatcherWrapper extends MatchingOpenMethodDispatcher implements IOpenMethodWrapper{
    MatchingOpenMethodDispatcher delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public MatchingOpenMethodDispatcherWrapper(XlsModuleOpenClass xlsModuleOpenClass, MatchingOpenMethodDispatcher delegate) {
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(xlsModuleOpenClass, this, target, params, env);
    }

    public IOpenMethod getDecisionTableOpenMethod() {
        return delegate.getDecisionTableOpenMethod();
    }

    public void setDecisionTableOpenMethod(IOpenMethod decisionTableOpenMethod) {
        delegate.setDecisionTableOpenMethod(decisionTableOpenMethod);
    }

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }
    
    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    public void addMethod(IOpenMethod candidate) {
        delegate.addMethod(candidate);
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public String getName() {
        return delegate.getName();
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public TableSyntaxNode getDispatcherTable() {
        return delegate.getDispatcherTable();
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public String toString() {
        return delegate.toString();
    }

    public IOpenMethod getTargetMethod() {
        return delegate.getTargetMethod();
    }

    public List<IOpenMethod> getCandidates() {
        return delegate.getCandidates();
    }
    
    public IOpenMethod findMatchingMethod(IRuntimeEnv env) {
        return delegate.findMatchingMethod(env);
    }
    
}
