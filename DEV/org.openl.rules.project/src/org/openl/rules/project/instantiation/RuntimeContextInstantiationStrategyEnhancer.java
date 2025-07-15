package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.runtime.ASMProxyFactory;

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
public class RuntimeContextInstantiationStrategyEnhancer implements RulesInstantiationStrategy {

    private final Logger log = LoggerFactory.getLogger(RuntimeContextInstantiationStrategyEnhancer.class);

    /**
     * Instantiation strategy delegate.
     */
    private final RulesInstantiationStrategy instantiationStrategy;

    /**
     * Internal generated class at runtime which used as service class.
     */
    private Class<?> serviceClass;

    public RuntimeContextInstantiationStrategyEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    /**
     * Gets enhanced service class.
     *
     * @return service class
     */
    @Override
    public final Class<?> getServiceClass() {
        if (serviceClass == null) {
            try {
                Class<?> originalServiceClass = instantiationStrategy.getInstanceClass();
                Class<?> result;
                ClassLoader classLoader = getClassLoader();
                try {
                    result = RuntimeContextInstantiationStrategyEnhancerHelper.decorateClass(originalServiceClass, classLoader);
                } catch (Exception e) {
                    throw new OpenlNotCheckedException("Failed to add runtime context in parameters of each method.", e);
                }
                serviceClass = result;
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to enhance a service class.", e);
            }
        }
        return serviceClass;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        if (RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(serviceClass)) {
            this.serviceClass = serviceClass;
                try {
                    ClassLoader classLoader = getClassLoader();
                    var clazz = RuntimeContextInstantiationStrategyEnhancerHelper.undecorateClass(serviceClass, classLoader);
                    instantiationStrategy.setServiceClass(clazz);
                } catch (Exception e) {
                    throw new OpenlNotCheckedException("Failed to remove runtime context from parameters of each method.", e);
                }
        } else {
            throw new OpenlNotCheckedException(
                    "Failed to set service class to instantiation strategy enhancer. Service class is not supported by this strategy.");
        }
    }

    @Override
    public final Object instantiate(boolean ignoreCompilationErrors) throws RulesInstantiationException {
        try {
            Object originalInstance = instantiationStrategy.instantiate(ignoreCompilationErrors);
            Class<?> originalServiceClass = instantiationStrategy.getInstanceClass();

            Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(), originalServiceClass);
            List<Class<?>> proxyInterfaces = new ArrayList<>();
            proxyInterfaces.add(getServiceClass());
            for (Class<?> interfaceClass : originalInstance.getClass().getInterfaces()) {
                if (!interfaceClass.equals(originalServiceClass)) {
                    proxyInterfaces.add(interfaceClass);
                }
            }
            return ASMProxyFactory.newProxyInstance(getClassLoader(),
                    new RuntimeContextInstantiationStrategyEnhancerInvocationHandler(methodsMap, originalInstance),
                    proxyInterfaces.toArray(new Class<?>[]{}));
        } catch (Exception e) {
            throw new RulesInstantiationException(e.getMessage(), e);
        }
    }

    @Override
    public final Object instantiate() throws RulesInstantiationException {
        return instantiate(false);
    }

    @Override
    public void reset() {
        instantiationStrategy.reset();
        serviceClass = null;
    }

    @Override
    public ClassLoader getClassLoader() throws RulesInstantiationException {
        return instantiationStrategy.getInstanceClass().getClassLoader();
    }

    @Override
    public final Class<?> getInstanceClass() {
        return getServiceClass();
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        return instantiationStrategy.compile();
    }

    @Override
    public void forcedReset() {
        reset();
        instantiationStrategy.forcedReset();
    }

    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        instantiationStrategy.setExternalParameters(parameters);
    }


    /**
     * Gets methods map where keys are interface class methods and values - original service class methods.
     *
     * @param interfaceClass class to expose as service class
     * @param serviceClass   original service class
     * @return methods map
     */
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) {
        log.debug("Creating methods map for classes: {} <-> {}", interfaceClass, serviceClass);

        Map<Method, Method> methodMap = new HashMap<>();
        Method[] serviceMethods = serviceClass.getDeclaredMethods();

        for (Method serviceMethod : serviceMethods) {

            String interfaceMethodName = serviceMethod.getName();
            Class<?>[] serviceMethodParameterTypes = serviceMethod.getParameterTypes();

            Class<?>[] newParams = new Class<?>[]{IRulesRuntimeContext.class};
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
