package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Objects;

import lombok.Getter;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractMatchingOpenMethodDispatcherWrapper;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public final class MatchingOpenMethodDispatcherWrapper extends AbstractMatchingOpenMethodDispatcherWrapper implements IRulesMethodWrapper {

    @Getter
    private final XlsModuleOpenClass xlsModuleOpenClass;
    @Getter
    private final ContextPropertiesInjector contextPropertiesInjector;
    @Getter
    private IOpenClass type;
    private IMethodSignature methodSignature;
    private final TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(
            this);
    private final boolean externalMethodCall;

    public MatchingOpenMethodDispatcherWrapper(XlsModuleOpenClass xlsModuleOpenClass,
                                               MatchingOpenMethodDispatcher delegate,
                                               ContextPropertiesInjector contextPropertiesInjector,
                                               boolean externalMethodCall) {
        super(delegate);
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass cannot be null");
        this.contextPropertiesInjector = contextPropertiesInjector;
        this.type = WrapperLogic.buildMethodReturnType(delegate, xlsModuleOpenClass);
        this.methodSignature = WrapperLogic.buildMethodSignature(delegate, xlsModuleOpenClass);
        this.externalMethodCall = externalMethodCall;
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(this, target, params, env, externalMethodCall);
    }

    @Override
    public void addMethod(IOpenMethod candidate) {
        delegate.addMethod(candidate);
        this.type = WrapperLogic.buildMethodReturnType(delegate, xlsModuleOpenClass);
        this.methodSignature = WrapperLogic.buildMethodSignature(delegate, xlsModuleOpenClass);
    }

    @Override
    public IMethodSignature getSignature() {
        return methodSignature;
    }

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (MatchingOpenMethodDispatcherWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
