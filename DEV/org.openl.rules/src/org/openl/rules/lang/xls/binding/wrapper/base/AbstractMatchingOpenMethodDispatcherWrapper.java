package org.openl.rules.lang.xls.binding.wrapper.base;

import java.util.List;
import java.util.Objects;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public abstract class AbstractMatchingOpenMethodDispatcherWrapper extends MatchingOpenMethodDispatcher {
    static {
        WrapperValidation.validateWrapperClass(AbstractMatchingOpenMethodDispatcherWrapper.class,
            AbstractMatchingOpenMethodDispatcherWrapper.class.getSuperclass());
    }

    protected final MatchingOpenMethodDispatcher delegate;

    public AbstractMatchingOpenMethodDispatcherWrapper(MatchingOpenMethodDispatcher delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
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
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public void addMethod(IOpenMethod candidate) {
        delegate.addMethod(candidate);
    }

    @Override
    public TableSyntaxNode getDispatcherTable() {
        return delegate.getDispatcherTable();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public List<IOpenMethod> getCandidates() {
        return delegate.getCandidates();
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
    public boolean isConstructor() {
        return delegate.isConstructor();
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
        return this;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegate.invoke(target, params, env);
    }

    @Override
    public IOpenMethod findMatchingMethod(IRuntimeEnv env) {
        return delegate.findMatchingMethod(env);
    }

    @Override
    public IOpenMethod getTargetMethod() {
        return delegate.getTargetMethod();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractMatchingOpenMethodDispatcherWrapper that = (AbstractMatchingOpenMethodDispatcherWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}