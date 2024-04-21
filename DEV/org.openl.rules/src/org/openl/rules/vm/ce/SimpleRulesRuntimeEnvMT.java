package org.openl.rules.vm.ce;

import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SimpleRulesRuntimeEnvMT extends SimpleRulesRuntimeEnv {
    private final SimpleRulesRuntimeEnv delegate;

    public SimpleRulesRuntimeEnvMT(SimpleRulesRuntimeEnv delegate) {
        this.delegate = delegate;
        pushThis(delegate.getThis());
        this.contextStack = delegate.cloneContextStack();
        pushContext(delegate.getContext());
        pushLocalFrame(delegate.getLocalFrame());
    }

    @Override
    public IRuntimeEnv clone() {
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
