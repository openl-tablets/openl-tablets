package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextConsumer;
import org.openl.runtime.AbstractOpenLMethodHandler;
import org.openl.runtime.IEngineWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of {@link InvocationHandler} which used by {@link RuntimeContextInstantiationStrategyEnhancer}
 * class to construct proxy of service class.
 */
class RuntimeContextInstantiationStrategyEnhancerInvocationHandler extends AbstractOpenLMethodHandler<Method, Method> {

    private final Logger log = LoggerFactory
        .getLogger(RuntimeContextInstantiationStrategyEnhancerInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    private Object serviceClassInstance;

    @Override
    public Method getTargetMember(Method key) {
        return methodsMap.get(key);
    }

    public RuntimeContextInstantiationStrategyEnhancerInvocationHandler(Map<Method, Method> methodsMap,
            Object serviceClassInstance) {
        this.methodsMap = methodsMap;
        this.serviceClassInstance = serviceClassInstance;
    }

    @Override
    public Object getTarget() {
        return serviceClassInstance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        Method member = methodsMap.get(method);

        if (member == null) {
            log.debug("Invoking not service class method: {} -> {}", method, method);

            return method.invoke(serviceClassInstance, args);
        }

        log.debug("Invoking service class method: {} -> {}", method, member);
        IRulesRuntimeContext context = (IRulesRuntimeContext) args[0];
        Object[] methodArgs = ArrayUtils.remove(args, 0);

        applyRulesRuntimeContext(serviceClassInstance, context);

        return member.invoke(serviceClassInstance, methodArgs);
    }

    private void applyRulesRuntimeContext(Object serviceInstance, IRulesRuntimeContext context) {

        Class<?> serviceClass = serviceInstance.getClass();

        if (IEngineWrapper.class.isAssignableFrom(serviceClass)) {
            log.debug("Applying runtime context: {} through IEngineWrapper instance", context);

            IEngineWrapper wrapper = (IEngineWrapper) serviceInstance;
            wrapper.getRuntimeEnv().setContext(context);
        } else if (IRulesRuntimeContextConsumer.class.isAssignableFrom(serviceClass)) {
            log.debug("Applying runtime context: {} through IRulesRuntimeContextConsumer instance", context);

            IRulesRuntimeContextConsumer wrapper = (IRulesRuntimeContextConsumer) serviceInstance;
            wrapper.setRuntimeContext(context);
        } else {
            log.error(
                "Failed to define rules runtime context for service instance. Service class must be instance one of: IEngineWrapper.class, IRulesRuntimeContextConsumer.class");
        }
    }

}
