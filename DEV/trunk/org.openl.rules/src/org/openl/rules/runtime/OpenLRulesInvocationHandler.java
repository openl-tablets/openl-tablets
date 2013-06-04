package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
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
            runtimeContext = new DefaultRulesRuntimeContext();
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
        if (method.getDeclaringClass().equals(IRulesRuntimeContextProvider.class)) {
            Method declaredMethod = OpenLRulesInvocationHandler.class.getDeclaredMethod(method.getName(), new Class<?>[0]);
            return declaredMethod.invoke(this, args);
        }
        return super.invoke(proxy, method, args);
    }
}
