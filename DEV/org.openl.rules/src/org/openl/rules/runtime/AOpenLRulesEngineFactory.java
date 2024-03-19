package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.AEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public abstract class AOpenLRulesEngineFactory extends AEngineFactory {

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

    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class<?>[]{IEngineWrapper.class, IRulesRuntimeContextProvider.class};
    }

    @Override
    protected final IOpenLMethodHandler prepareMethodHandler(Object openClassInstance,
                                                             Map<Method, IOpenMember> methodMap,
                                                             IRuntimeEnv runtimeEnv) {
        OpenLRulesMethodHandler openLRulesMethodHandler = new OpenLRulesMethodHandler(openClassInstance,
                methodMap,
                getRuntimeEnvBuilder());
        if (runtimeEnv != null) {
            openLRulesMethodHandler.setRuntimeEnv(runtimeEnv);
        }
        return openLRulesMethodHandler;
    }
}
