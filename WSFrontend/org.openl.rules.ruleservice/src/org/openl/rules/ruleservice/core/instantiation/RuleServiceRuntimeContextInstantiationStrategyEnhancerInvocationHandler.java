package org.openl.rules.ruleservice.core.instantiation;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.rules.ruleservice.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.context.RuntimeContextConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public class RuleServiceRuntimeContextInstantiationStrategyEnhancerInvocationHandler implements InvocationHandler {
    private final Logger log = LoggerFactory.getLogger(RuleServiceRuntimeContextInstantiationStrategyEnhancerInvocationHandler.class);

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
            log.debug("Invoking not service class method: {} -> {}", method, method);

            return method.invoke(serviceClassInstance, args);
        }

        log.debug("Invoking method of service class: {} -> {}", method, member);
        IRulesRuntimeContext context = (IRulesRuntimeContext) args[0];
        Object[] methodArgs = ArrayUtils.clone(args);
        methodArgs[0] = (org.openl.rules.context.IRulesRuntimeContext) RuntimeContextConvertor.covert(context);

        return member.invoke(serviceClassInstance, methodArgs);
    }
}
