package org.openl.rules.ruleservice.core.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.context.RuntimeContextConvertor;

public class RuleServiceRuntimeContextInstantiationStrategyEnhancerInvocationHandler implements InvocationHandler {
    private final Log log = LogFactory
            .getLog(RuleServiceRuntimeContextInstantiationStrategyEnhancerInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    private Object serviceClassInstance;

    public RuleServiceRuntimeContextInstantiationStrategyEnhancerInvocationHandler(Map<Method, Method> methodsMap,
            Object serviceClassInstance) {
        this.methodsMap = methodsMap;
        this.serviceClassInstance = serviceClassInstance;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method member = methodsMap.get(method);

        if (member == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Invoking not service class method: %s -> %s", method.toString(),
                        method.toString()));
            }

            return method.invoke(serviceClassInstance, args);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Invoking service class method: %s -> %s", method.toString(), member.toString()));
        }
        IRulesRuntimeContext context = (IRulesRuntimeContext) args[0];
        Object[] methodArgs = ArrayUtils.clone(args);
        methodArgs[0] = (org.openl.rules.context.IRulesRuntimeContext) RuntimeContextConvertor.covert(context);

        return member.invoke(serviceClassInstance, methodArgs);
    }
}
