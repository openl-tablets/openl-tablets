package org.openl.rules.ruleservice.publish.lazy.wrapper;

import java.util.Objects;

import org.openl.rules.lang.xls.binding.wrapper.base.AbstractAlgorithmWrapper;
import org.openl.rules.lang.xls.prebind.ILazyMethod;
import org.openl.rules.ruleservice.publish.lazy.LazyMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public final class AlgorithmLazyWrapper extends AbstractAlgorithmWrapper implements ILazyMethod {
    private final LazyMethod lazyMethod;

    public AlgorithmLazyWrapper(LazyMethod lazyMethod, Algorithm delegate) {
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
        AlgorithmLazyWrapper that = (AlgorithmLazyWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}