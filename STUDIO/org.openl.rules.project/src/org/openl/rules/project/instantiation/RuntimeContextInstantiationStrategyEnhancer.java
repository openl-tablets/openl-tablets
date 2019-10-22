package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auxiliary class which enhances rule service with ability to use rule service method with rules runtime context during
 * method invocation. The class is used by engine to expose rules services as web service.
 * <p/>
 * Enhancer class decorates methods of original service class and exposes new methods signatures instead of original
 * methods. New method signature has one more parameter - rules runtime context. While service method invocation engine
 * do the following steps:
 * <ul>
 * <li>recognize context parameter;</li>
 * <li>recognize original service method to invoke using method signature;</li>
 * <li>set rules runtime context;</li>
 * <li>invoke appropriate service method.</li>
 * </ul>
 *
 * @author Marat Kamalov
 */
public class RuntimeContextInstantiationStrategyEnhancer extends AbstractServiceClassEnhancerInstantiationStrategy {

    private final Logger log = LoggerFactory.getLogger(RuntimeContextInstantiationStrategyEnhancer.class);

    /**
     * Constructs new instance of instantiation strategy.
     *
     * @param instantiationStrategy instantiation strategy which used to instantiate original service
     */
    public RuntimeContextInstantiationStrategyEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    @Override
    protected Class<?> decorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return RuntimeContextInstantiationStrategyEnhancerHelper.decorateClass(serviceClass, classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to add runtime context in parameters of each method.", e);
        }
    }

    @Override
    protected Class<?> undecorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return RuntimeContextInstantiationStrategyEnhancerHelper.undecorateClass(serviceClass, classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to remove runtime context from parameters of each method.", e);
        }
    }

    @Override
    protected boolean validateServiceClass(Class<?> serviceClass) throws ValidationServiceClassException {
        if (RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(serviceClass)) {
            return true;
        } else {
            throw new ValidationServiceClassException("Runtime context parameter is required in each method.");
        }
    }

    /**
     * Makes invocation handler.
     *
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    @Override
    protected InvocationHandler makeInvocationHandler(Object instanceObject) throws Exception {
        Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(),
            getOriginalInstantiationStrategy().getInstanceClass());
        return new RuntimeContextInstantiationStrategyEnhancerInvocationHandler(methodsMap, instanceObject);
    }

    /**
     * Gets methods map where keys are interface class methods and values - original service class methods.
     *
     * @param interfaceClass class to expose as service class
     * @param serviceClass original service class
     * @return methods map
     */
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) {
        log.debug("Creating methods map for classes: {} <-> {}", interfaceClass, serviceClass);

        Map<Method, Method> methodMap = new HashMap<>();
        Method[] serviceMethods = serviceClass.getDeclaredMethods();

        for (Method serviceMethod : serviceMethods) {

            String interfaceMethodName = serviceMethod.getName();
            Class<?>[] serviceMethodParameterTypes = serviceMethod.getParameterTypes();

            Class<?>[] newParams = new Class<?>[] { IRulesRuntimeContext.class };
            Class<?>[] extendedParamTypes = ArrayUtils.addAll(newParams, serviceMethodParameterTypes);

            Method interfaceMethod;
            try {
                interfaceMethod = interfaceClass.getMethod(interfaceMethodName, extendedParamTypes);
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
