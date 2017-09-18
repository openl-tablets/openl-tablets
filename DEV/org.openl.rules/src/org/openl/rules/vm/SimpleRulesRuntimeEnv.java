package org.openl.rules.vm;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class SimpleRulesRuntimeEnv extends SimpleRuntimeEnv {
    private volatile boolean methodArgumentsCacheEnable = false;
    private volatile CacheMode cacheMode = CacheMode.READ_ONLY;
    private volatile boolean ignoreRecalculate = true;
    private volatile boolean originalCalculation = true;
    private ArgumentCachingStorage argumentCachingStorage = new ArgumentCachingStorage();
    
    public SimpleRulesRuntimeEnv() {
        super();
    }
    
    private SimpleRulesRuntimeEnv(SimpleRulesRuntimeEnv env) {
        super();
        this.argumentCachingStorage = env.argumentCachingStorage;
        this.methodArgumentsCacheEnable = env.methodArgumentsCacheEnable;
        this.cacheMode = env.cacheMode;
        this.ignoreRecalculate = env.ignoreRecalculate;
        this.originalCalculation = env.originalCalculation;
    }

    @Override
    public IRuntimeEnv cloneEnvForMT() {
        return new SimpleRulesRuntimeEnv(this);
    }

    @Override
    protected IRuntimeContext buildDefaultRuntimeContext() {
        return RulesRuntimeContextFactory.buildRulesRuntimeContext();
    }
    
    public boolean isMethodArgumentsCacheEnable() {
        return methodArgumentsCacheEnable;
    }

    public void changeMethodArgumentsCacheMode(CacheMode mode) {
        this.cacheMode = mode;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setMethodArgumentsCacheEnable(boolean enable) {
        this.methodArgumentsCacheEnable = enable;
    }
    
    public boolean isIgnoreRecalculation() {
        return ignoreRecalculate;
    }

    public void setIgnoreRecalculate(boolean ignoreRecalculate) {
        this.ignoreRecalculate = ignoreRecalculate;
    }

    public boolean isOriginalCalculation() {
        return originalCalculation;
    }

    public void setOriginalCalculation(boolean originalCalculation) {
        this.originalCalculation = originalCalculation;
    }

    public ArgumentCachingStorage getArgumentCachingStorage() {
        return argumentCachingStorage;
    }

    public IOpenClass getTopClass() {
        return topClass;
    }
    
    // This is workaround of too much creating MethodKeys in runtime
    private Map<IOpenMethodWrapper, IOpenMethod> wrapperMethodCache = new HashMap<IOpenMethodWrapper, IOpenMethod>();

    public IOpenMethod getTopClassMethod(IOpenMethodWrapper wrapper) {
        if (topClass == null) {
            return null;
        }
        IOpenMethod method = wrapperMethodCache.get(wrapper);
        if (method != null) {
            return method;
        }
        method = topClass.getMethod(wrapper.getDelegate().getName(),
            wrapper.getDelegate().getSignature().getParameterTypes());
        wrapperMethodCache.put(wrapper, method);
        return method;
    }

    public void setTopClass(IOpenClass topClass) {
        this.topClass = topClass;
    }

    private IOpenClass topClass;
}
