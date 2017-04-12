package org.openl.rules.ruleservice.core.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.AbstractServiceClassEnhancerInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.ValidationServiceClassException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marat Kamalov
 */
public class RuleServiceRuntimeContextInstantiationStrategyEnhancer extends AbstractServiceClassEnhancerInstantiationStrategy {
    private final Logger log = LoggerFactory.getLogger(RuleServiceRuntimeContextInstantiationStrategyEnhancer.class);

    /**
     * Constructs new instance of instantiation strategy.
     *
     * @param instantiationStrategy instantiation strategy which used to
     *            instantiate original service
     */
    public RuleServiceRuntimeContextInstantiationStrategyEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    @Override
    protected Class<?> decorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return RuleServiceRuntimeContextInstantiationStrategyEnhancerHelper.decorateClass(serviceClass, classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to replace runtime context in parameters of each method.", e);
        }
    }

    @Override
    protected Class<?> undecorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return RuleServiceRuntimeContextInstantiationStrategyEnhancerHelper.undecorateClass(serviceClass,
                    classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to replace runtime context in parameters of each method.", e);
        }
    }

    @Override
    protected boolean validateServiceClass(Class<?> serviceClass) throws ValidationServiceClassException {
        if (RuleServiceRuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(serviceClass)) {
            return true;
        } else {
            throw new ValidationServiceClassException("RuleService runtime context parameter required in each method!");
        }
    }

    /**
     * Makes invocation handler.
     *
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    protected InvocationHandler makeInvocationHandler(Object instanceObject) throws Exception {
        Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(),
            getOriginalInstantiationStrategy().getInstanceClass());
        return new RuleServiceRuntimeContextInstantiationStrategyEnhancerInvocationHandler(methodsMap, instanceObject);
    }

    /**
     * Gets methods map where keys are interface class methods and values -
     * original service class methods.
     *
     * @param interfaceClass class to expose as service class
     * @param serviceClass original service class
     * @return methods map
     */
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) {
        log.debug("Creating methods map for classes: {} <-> {}.", interfaceClass, serviceClass);

        Map<Method, Method> methodMap = new HashMap<Method, Method>();
        Method[] serviceMethods = serviceClass.getDeclaredMethods();

        for (Method serviceMethod : serviceMethods) {

            String interfaceMethodName = serviceMethod.getName();
            Class<?>[] parameterTypes = serviceMethod.getParameterTypes();

            parameterTypes[0] = org.openl.rules.ruleservice.context.IRulesRuntimeContext.class;

            Method interfaceMethod;
            try {
                interfaceMethod = interfaceClass.getMethod(interfaceMethodName, parameterTypes);
                methodMap.put(interfaceMethod, serviceMethod);
            } catch (NoSuchMethodException e) {
                // Ignore an exception. Interface class can ignore several
                // methods what declared in service class.
            }
        }

        log.debug("{}", methodMap);

        return methodMap;
    }
}
