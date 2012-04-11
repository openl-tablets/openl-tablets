package org.openl.rules.ruleservice.factory;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.runtime.AEngineFactory;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class WebServiceRulesInvocationHandler extends OpenLInvocationHandler {

    public WebServiceRulesInvocationHandler(Object openlInstance,
            AEngineFactory engineFactory,
            IRuntimeEnv openlEnv,
            Map<Method, IOpenMember> methodMap) {

        super(openlInstance, engineFactory, openlEnv, methodMap);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        IOpenMember member = getMethodMap().get(method);
        
        if (member instanceof IOpenMethod) {
            IRulesRuntimeContext context = (IRulesRuntimeContext) args[0];
            getRuntimeEnv().setContext(context);
            Object[] methodArgs = ArrayUtils.remove(args, 0);
            
            return super.invoke(proxy, method, methodArgs);
        }
        
        return super.invoke(proxy, method, args);
    }

}
