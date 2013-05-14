package org.openl.rules.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.conf.IUserContext;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
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
    protected SimpleRulesRuntimeEnv makeDefaultRuntimeEnv() {
        return new SimpleRulesVM().getRuntimeEnv();
    }
    
    @Override
    public Object makeInstance(IRuntimeEnv runtimeEnv) {
        if (runtimeEnv == null || runtimeEnv instanceof SimpleRulesRuntimeEnv){
            return innerMakeInstance((SimpleRulesRuntimeEnv) runtimeEnv);    
        }
        throw new IllegalArgumentException("Subclasses of AOpenLRulesEngineFactory supports only SimpleRulesRuntimeEnv!!!");
    }
    
    public abstract Object innerMakeInstance(SimpleRulesRuntimeEnv runtimeEnv);
    
    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance, Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {
        return new OpenLRulesInvocationHandler(openClassInstance, runtimeEnv, methodMap);
    }
}
