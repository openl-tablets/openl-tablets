package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.runtime.OpenLMethodHandler;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

public class OpenLRulesMethodHandler extends OpenLMethodHandler implements IRulesRuntimeContextProvider {

    private final ValidationHandler validationHandler = new ValidationHandler();

    @Override
    public IRulesRuntimeContext getRuntimeContext() {
        return (IRulesRuntimeContext) getRuntimeEnv().getContext();
    }

    public OpenLRulesMethodHandler(Object openlInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnvBuilder runtimeEnvBuilder) {
        super(openlInstance, methodMap, runtimeEnvBuilder);
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        if (IRulesRuntimeContextProvider.class == method.getDeclaringClass()) {
            return method.invoke(this, args);
        }
        if (IEngineWrapper.class != method.getDeclaringClass()) {
            IOpenMember targetMethod = getMethodMap().get(method);
            if (targetMethod instanceof IOpenMethod) {
                validationHandler
                    .validateProxyArguments(((IOpenMethod) targetMethod).getSignature(), getRuntimeEnv(), args);
            }
        }
        return super.invoke(method, args);
    }
}
