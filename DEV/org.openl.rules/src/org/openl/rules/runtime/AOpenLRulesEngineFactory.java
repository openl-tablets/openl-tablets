package org.openl.rules.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.conf.IUserContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IRuntimeEnvBuilder;
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
    protected IRuntimeEnvBuilder getRuntimeEnvBuilder() {
        if (runtimeEnvBuilder == null) {
            runtimeEnvBuilder = () -> new SimpleRulesVM().getRuntimeEnv();
        }
        return runtimeEnvBuilder;
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class<?>[] { IEngineWrapper.class, IRulesRuntimeContextProvider.class };
    }

    @Override
    protected final InvocationHandler prepareInvocationHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {
        return new OpenLRulesInvocationHandler(openClassInstance, runtimeEnv, methodMap);
    }
}
