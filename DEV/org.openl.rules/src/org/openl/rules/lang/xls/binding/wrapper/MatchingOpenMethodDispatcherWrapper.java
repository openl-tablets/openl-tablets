package org.openl.rules.lang.xls.binding.wrapper;

import java.util.List;
import java.util.Objects;

import org.openl.rules.lang.xls.binding.ModuleRelatedType;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class MatchingOpenMethodDispatcherWrapper extends MatchingOpenMethodDispatcher implements IOpenMethodWrapper {
    static {
        WrapperLogic.validateWrapperClass(MatchingOpenMethodDispatcherWrapper.class, MatchingOpenMethodDispatcherWrapper.class.getSuperclass());
    }

    private final MatchingOpenMethodDispatcher delegate;
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ContextPropertiesInjector contextPropertiesInjector;
    private final IOpenClass type;
    private final IMethodSignature methodSignature;

    public MatchingOpenMethodDispatcherWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            MatchingOpenMethodDispatcher delegate,
            ContextPropertiesInjector contextPropertiesInjector) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass cannot be null");
        this.contextPropertiesInjector = Objects.requireNonNull(contextPropertiesInjector,
            "contextPropertiesInjector cannot be null");
        if (delegate.getType() instanceof ModuleRelatedType) {
            IOpenClass type = xlsModuleOpenClass.findType(delegate.getType().getName());
            this.type = type != null ? type : delegate.getType();
        } else {
            this.type = delegate.getType();
        }
        this.methodSignature = WrapperLogic.buildMethodSignature(delegate, xlsModuleOpenClass);
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
    public IMethodSignature getSignature() {
        return methodSignature;
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
        return type;
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
        return this;
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

    private final TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

    @Override
    public ContextPropertiesInjector getContextPropertiesInjector() {
        return contextPropertiesInjector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MatchingOpenMethodDispatcherWrapper that = (MatchingOpenMethodDispatcherWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
