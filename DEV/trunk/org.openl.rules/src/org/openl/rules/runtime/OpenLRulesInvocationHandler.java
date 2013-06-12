package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public class OpenLRulesInvocationHandler extends OpenLInvocationHandler{
    public OpenLRulesInvocationHandler(Object openlInstance,
            IRuntimeEnv openlEnv,
            Map<Method, IOpenMember> methodMap) {
        super(openlInstance, openlEnv, methodMap);
    }
    
    public OpenLRulesInvocationHandler(Object openlInstance,
            Map<Method, IOpenMember> methodMap) {
        super(openlInstance, methodMap);
    }
    
    @Override
    public IRuntimeEnv makeRuntimeEnv() {
        return new SimpleRulesVM().getRuntimeEnv();
    }
}
