package org.openl.rules.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.conf.IUserContext;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public abstract class AOpenLRulesEngineFactory extends AOpenLEngineFactory {

    public AOpenLRulesEngineFactory(String openlName, IUserContext userContext) {
        super(openlName, userContext);
    }

    public AOpenLRulesEngineFactory(String openlName, String userHome) {
        super(openlName, userHome);
    }

    public AOpenLRulesEngineFactory(String openlName) {
        super(openlName);
    }

    @Override
    protected ThreadLocal<IRuntimeEnv> initRuntimeEnvironment() {
        return new RuntimeEnvHolder();

    }
    
    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance, Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {
        return new OpenLRulesInvocationHandler(openClassInstance, this, runtimeEnv, methodMap);
    }

    // ThreadLocals can be cached by servlet container. RuntimeEnvHolder should
    // be nested class, not inner class - otherwise we get memory leak
    private static final class RuntimeEnvHolder extends ThreadLocal<org.openl.vm.IRuntimeEnv> {
        @Override
        protected org.openl.vm.IRuntimeEnv initialValue() {
            return new SimpleRulesVM().getRuntimeEnv();
        }
    }
}
