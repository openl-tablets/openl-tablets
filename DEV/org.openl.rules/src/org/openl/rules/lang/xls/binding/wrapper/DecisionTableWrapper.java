package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Objects;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractDecisionTableWrapper;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public final class DecisionTableWrapper extends AbstractDecisionTableWrapper implements IRulesMethodWrapper {

    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ContextPropertiesInjector contextPropertiesInjector;
    private final IOpenClass type;
    private final IMethodSignature methodSignature;
    private final TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(
        this);
    private final boolean inlinedMethodCall;

    public DecisionTableWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            DecisionTable delegate,
            ContextPropertiesInjector contextPropertiesInjector,
            boolean inlinedMethodCall) {
        super(delegate);
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass cannot be null");
        this.contextPropertiesInjector = contextPropertiesInjector;
        this.type = WrapperLogic.buildMethodReturnType(delegate, xlsModuleOpenClass);
        this.methodSignature = WrapperLogic.buildMethodSignature(delegate, xlsModuleOpenClass);
        this.inlinedMethodCall = inlinedMethodCall;
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (inlinedMethodCall) {
            return WrapperLogic.invokeInlinedMethod(this, target, params, env);
        }
        return WrapperLogic.invoke(this, target, params, env);
    }

    @Override
    public XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClass;
    }

    @Override
    public IMethodSignature getSignature() {
        return methodSignature;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

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
        DecisionTableWrapper that = (DecisionTableWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
