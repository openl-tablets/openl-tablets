package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.AbstractServiceClassEnhancerInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.ValidationServiceClassException;
import org.openl.runtime.ASMProxyHandler;

/**
 * Auxiliary class for support of variations.
 * <p/>
 * It uses specified service class or generated one to map methods with injected variations to original methods of
 * compiled rules.
 *
 * @author PUdalau, Marat Kamalov
 */
@Deprecated
public class VariationInstantiationStrategyEnhancer extends AbstractServiceClassEnhancerInstantiationStrategy {

    private final Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancer.class);

    /**
     * Constructs new instance of variations enhancer.
     *
     * @param instantiationStrategy instantiation strategy which used to instantiate original service
     */
    public VariationInstantiationStrategyEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    @Override
    protected Class<?> decorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return VariationInstantiationStrategyEnhancerHelper.decorateClass(serviceClass, classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException(
                    String.format("Failed to inject variation parameters into methods in the interface '%s'.",
                            serviceClass.getTypeName()),
                    e);
        }
    }

    @Override
    protected boolean validateServiceClass(Class<?> serviceClass) throws ValidationServiceClassException {
        if (VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(serviceClass)) {
            return true;
        } else {
            throw new ValidationServiceClassException(String.format(
                    "Variation result return type and variation pack parameter is required for each method in the interface '%s'.",
                    serviceClass.getTypeName()));
        }
    }

    @Override
    protected Class<?> undecorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return VariationInstantiationStrategyEnhancerHelper.undecorateClass(serviceClass, classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException(
                    String.format("Failed to remove variation parameters from methods in the interface '%s'.",
                            serviceClass.getTypeName()),
                    e);
        }
    }

    /**
     * Makes invocation handler.
     *
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    @Override
    protected ASMProxyHandler makeMethodHandler(Object originalInstance) throws Exception {
        Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(),
                getOriginalInstantiationStrategy().getInstanceClass());
        return new VariationInstantiationStrategyEnhancerInvocationHandler(methodsMap, originalInstance);
    }

    /**
     * Gets methods map where keys are interface class methods and values - original service class methods.
     *
     * @param interfaceClass class to expose as service class
     * @param serviceClass   original service class
     * @return methods map
     */
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) throws Exception {

        log.debug("Creating methods map for classes: {} <-> {}", interfaceClass, serviceClass);

        Map<Method, Method> methodMap = new HashMap<>();
        Method[] serviceMethods = interfaceClass.getDeclaredMethods();
        for (Method serviceMethod : serviceMethods) {
            try {
                Method originalMethod = VariationInstantiationStrategyEnhancerHelper
                        .getMethodForDecoration(serviceClass, serviceMethod);
                methodMap.put(serviceMethod, originalMethod);
            } catch (Exception e) {
                throw new RulesInstantiationException(
                        "Failed to find corresponding method in original class for method '" + MethodUtil
                                .printMethod(serviceMethod.getName() + "'.", serviceMethod.getParameterTypes()));
            }
        }

        log.debug("{}", methodMap);

        return methodMap;
    }

}
