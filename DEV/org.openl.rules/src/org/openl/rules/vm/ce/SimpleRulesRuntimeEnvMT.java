package org.openl.rules.vm.ce;

import org.openl.vm.SimpleRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SimpleRulesRuntimeEnvMT extends SimpleRuntimeEnv {
    private final SimpleRuntimeEnv delegate;

    public SimpleRulesRuntimeEnvMT(SimpleRuntimeEnv delegate) {
        this.delegate = delegate;
        pushThis(delegate.getThis());
        this.contextStack = delegate.cloneContextStack();
        pushContext(delegate.getContext());
        pushLocalFrame(delegate.getLocalFrame());
    }

    @Override
    public IRuntimeEnv clone() {
        return copy();
    }

    @Override
    public SimpleRulesRuntimeEnvMT copy() {
        return new SimpleRulesRuntimeEnvMT(this);
    }

    @Override
    public IOpenClass getTopClass() {
        return delegate.getTopClass();
    }

    @Override
    public void setTopClass(IOpenClass topClass) {
        throw new UnsupportedOperationException();
    }
}
