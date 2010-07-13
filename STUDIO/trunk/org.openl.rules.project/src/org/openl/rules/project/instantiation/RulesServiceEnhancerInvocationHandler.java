package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextConsumer;
import org.openl.runtime.IEngineWrapper;

/**
 * The implementation of {@link InvocationHandler} which used by
 * {@link RulesServiceEnhancer} class to construct proxy of service class.
 * 
 */
class RulesServiceEnhancerInvocationHandler implements InvocationHandler {

    private static final Log LOG = LogFactory.getLog(RulesServiceEnhancerInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    private Object serviceClassInstance;

    public RulesServiceEnhancerInvocationHandler(Map<Method, Method> methodsMap, Object serviceClassInstance) {
        this.methodsMap = methodsMap;
        this.serviceClassInstance = serviceClassInstance;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Method member = methodsMap.get(method);

        LOG.debug(String.format("Invoking service class method: %s -> %s", method.toString(), member.toString()));

        IRulesRuntimeContext context = (IRulesRuntimeContext) args[0];
        Object[] methodArgs = ArrayUtils.remove(args, 0);

        applyRulesRuntimeContext(serviceClassInstance, context);

        return member.invoke(serviceClassInstance, methodArgs);
    }

    private void applyRulesRuntimeContext(Object serviceInstance, IRulesRuntimeContext context) {

        Class<? extends Object> serviceClass = serviceInstance.getClass();

        if (IEngineWrapper.class.isAssignableFrom(serviceClass)) {

            LOG.debug(String.format("Applying runtime context: %s thru IEngineWrapper instance", context.toString()));

            IEngineWrapper wrapper = (IEngineWrapper) serviceInstance;
            wrapper.getRuntimeEnv().setContext(context);
        } else if (IRulesRuntimeContextConsumer.class.isAssignableFrom(serviceClass)) {

            LOG.debug(String.format("Applying runtime context: %s thru IRulesRuntimeContextConsumer instance",
                context.toString()));

            IRulesRuntimeContextConsumer wrapper = (IRulesRuntimeContextConsumer) serviceInstance;
            wrapper.setRuntimeContext(context);
        } else {

            LOG.error("Cannot define rules runtime context for service instance. Service class must be instance one of: IEngineWrapper.class, IRulesRuntimeContextConsumer.class");

            // throw new
            // RuntimeException("Cannot define rules runtime context for service instance.");
        }
    }

}
