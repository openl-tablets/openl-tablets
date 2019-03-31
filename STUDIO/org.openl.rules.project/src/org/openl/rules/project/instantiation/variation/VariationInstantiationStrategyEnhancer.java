package org.openl.rules.project.instantiation.variation;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.AbstractServiceClassEnhancerInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.ValidationServiceClassException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Auxiliary class for support of variations.
 * <p/>
 * It uses specified service class or generated one to map methods with injected
 * variations to original methods of compiled rules.
 *
 * @author PUdalau, Marat Kamalov
 */
public class VariationInstantiationStrategyEnhancer extends AbstractServiceClassEnhancerInstantiationStrategy {

    private final Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancer.class);

    /**
     * Constructs new instance of variations enhancer.
     *
     * @param instantiationStrategy instantiation strategy which used to
     *                              instantiate original service
     */
    public VariationInstantiationStrategyEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    @Override
    protected Class<?> decorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return VariationInstantiationStrategyEnhancerHelper.decorateClass(serviceClass, classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to inject variation in parameters of each method.", e);
        }
    }

    @Override
    protected boolean validateServiceClass(Class<?> serviceClass) throws ValidationServiceClassException {
        if (VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(serviceClass)) {
            return true;
        } else {
            throw new ValidationServiceClassException("Variation result return type and variation pack parameter is required in each variation method!");
        }
    }

    @Override
    protected Class<?> undecorateServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        try {
            return VariationInstantiationStrategyEnhancerHelper.undecorateClass(serviceClass,
                    classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to remove variation methods.", e);
        }
    }

    /**
     * Makes invocation handler.
     *
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    @Override
    protected InvocationHandler makeInvocationHandler(Object originalInstance) throws Exception {
        Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(),
                getOriginalInstantiationStrategy().getInstanceClass());
        return new VariationInstantiationStrategyEnhancerInvocationHandler(methodsMap, originalInstance);
    }

    /**
     * Gets methods map where keys are interface class methods and values -
     * original service class methods.
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
                Method originalMethod = VariationInstantiationStrategyEnhancerHelper.getMethodForDecoration(serviceClass, serviceMethod);
                methodMap.put(serviceMethod, originalMethod);
            } catch (Exception e) {
                throw new RulesInstantiationException("Failed to find corresrponding method in original class for method '" + MethodUtil.printMethod(serviceMethod.getName() + "'!",
                        serviceMethod.getParameterTypes()));
            }
        }

        log.debug("{}", methodMap);

        return methodMap;
    }

}
