package org.openl.rules.vm.ce;

import org.openl.rules.vm.ArgumentCachingStorage;
import org.openl.rules.vm.CacheMode;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SimpleRulesRuntimeEnvMT extends SimpleRulesRuntimeEnv {
    private SimpleRulesRuntimeEnv delegate;

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

    @Override
    public boolean isMethodArgumentsCacheEnable() {
        return false;
    }

    @Override
    public void changeMethodArgumentsCacheMode(CacheMode mode) {
    }

    @Override
    public CacheMode getCacheMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMethodArgumentsCacheEnable(boolean enable) {
    }

    @Override
    public boolean isIgnoreRecalculation() {
        return true;
    }

    @Override
    public void setIgnoreRecalculate(boolean ignoreRecalculate) {
    }

    @Override
    public boolean isOriginalCalculation() {
        return true;
    }

    @Override
    public void setOriginalCalculation(boolean originalCalculation) {
    }

    @Override
    public ArgumentCachingStorage getArgumentCachingStorage() {
        throw new UnsupportedOperationException();
    }
}
