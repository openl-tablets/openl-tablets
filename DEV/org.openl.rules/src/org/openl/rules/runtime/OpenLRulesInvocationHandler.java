package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public class OpenLRulesInvocationHandler extends OpenLInvocationHandler implements IRulesRuntimeContextProvider {
    public OpenLRulesInvocationHandler(Object openlInstance, IRuntimeEnv openlEnv, Map<Method, IOpenMember> methodMap) {
        super(openlInstance, openlEnv, methodMap);
    }

    public IRulesRuntimeContext getRuntimeContext() {

        IRulesRuntimeContext runtimeContext = (IRulesRuntimeContext) getRuntimeEnv().getContext();

        if (runtimeContext == null) {
            runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
            getRuntimeEnv().setContext(runtimeContext);
        }

        return (IRulesRuntimeContext) runtimeContext;
    }

    public OpenLRulesInvocationHandler(Object openlInstance, Map<Method, IOpenMember> methodMap) {
        super(openlInstance, methodMap);
    }

    @Override
    public IRuntimeEnv makeRuntimeEnv() {
        return new SimpleRulesVM().getRuntimeEnv();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (IRulesRuntimeContextProvider.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        return super.invoke(proxy, method, args);
    }
}
