package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.AEngineFactory;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public class OpenLRulesInvocationHandler extends OpenLInvocationHandler{
    public OpenLRulesInvocationHandler(Object openlInstance,
            AEngineFactory engineFactory,
            IRuntimeEnv openlEnv,
            Map<Method, IOpenMember> methodMap) {
        super(openlInstance, engineFactory, openlEnv, methodMap);
    }
    
    @Override
    public IRuntimeEnv buildRuntimeEnv() {
        return new SimpleRulesVM().getRuntimeEnv();
    }
}
