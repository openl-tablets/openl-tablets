package org.openl.rules.ruleservice.publish.lazy.wrapper;

import java.util.Objects;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractDecisionTableWrapper;
import org.openl.rules.lang.xls.prebind.ILazyMethod;
import org.openl.rules.ruleservice.publish.lazy.LazyMethod;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public final class DecisionTableLazyWrapper extends AbstractDecisionTableWrapper implements ILazyMethod {
    private final LazyMethod lazyMethod;

    public DecisionTableLazyWrapper(LazyMethod lazyMethod, DecisionTable delegate) {
        super(delegate);
        this.lazyMethod = Objects.requireNonNull(lazyMethod, "lazyMethod cannot be null");
    }

    @Override
    public IOpenMethod getMember() {
        return lazyMethod.getMember();
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return lazyMethod.getMember().invoke(target, params, env);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DecisionTableLazyWrapper that = (DecisionTableLazyWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}