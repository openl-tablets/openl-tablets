package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyDelegator;

/**
 * Auxiliary class for support of variations.
 * 
 * It uses specified service class or generated one to map methods with injected
 * variations to original methods of compiled rules.
 * 
 * @author PUdalau
 */
public class VariationsEnhancer extends RulesInstantiationStrategyDelegator {

    private final Log LOG = LogFactory.getLog(VariationsEnhancer.class);

    /**
     * Internal generated class at runtime which used as service class.
     */
    private Class<?> serviceClass;

    /**
     * Constructs new instance of variations enhancer.
     * 
     * @param instantiationStrategy instantiation strategy which used to
     *            instantiate original service
     */
    public VariationsEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    /**
     * Gets enhanced with variations service class.
     * 
     * @return service class
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    public Class<?> getServiceClass() throws ClassNotFoundException {
        if (serviceClass == null) {
            try {
                Class<?> originalServiceClass = getOriginalInstantiationStrategy().getInstanceClass();
                serviceClass = VariationsEnhancerHelper.decorateMethods(originalServiceClass, getClassLoader());
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to inject variations to service class.", e);
            }
        }
        return serviceClass;
    }

    @Override
    public Object instantiate() throws RulesInstantiationException, ClassNotFoundException {
        try {
            InvocationHandler handler = makeInvocationHandler();
            return Proxy.newProxyInstance(getClassLoader(), getProxyInterfaces(), handler);
        } catch (Exception e) {
            throw new RulesInstantiationException(e.getMessage(), e);
        }
    }

    /**
     * Makes invocation handler.
     * 
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    private InvocationHandler makeInvocationHandler() throws Exception {
        Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(),
            getOriginalInstantiationStrategy().getInstanceClass());
        return new VariationsInvocationHandler(methodsMap, getOriginalInstantiationStrategy().instantiate());
    }

    /**
     * Gets interface classes what used for proxy construction.
     * 
     * @return proxy interfaces
     * @throws Exception
     */
    private Class<?>[] getProxyInterfaces() throws Exception {
        return new Class<?>[] { getServiceClass() };
    }
    
    /**
     * Gets methods map where keys are interface class methods and values -
     * original service class methods.
     * 
     * @param interfaceClass class to expose as service class
     * @param serviceClass original service class
     * @return methods map
     */
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) throws Exception{

        LOG.debug(String.format("Creating methods map for classes: %s <-> %s", interfaceClass, serviceClass));

        Map<Method, Method> methodMap = new HashMap<Method, Method>();
        Method[] serviceMethods = interfaceClass.getDeclaredMethods();

        for (Method serviceMethod : serviceMethods) {
            try {
                Method originalMethod = VariationsEnhancerHelper.getMethodForEnhanced(serviceClass, serviceMethod);
                methodMap.put(serviceMethod, originalMethod);
            } catch (Exception e) {
                throw new RulesInstantiationException("Failed to find corresrponding method in original class for method" + MethodUtil.printMethod(serviceMethod.getName(),
                    serviceMethod.getParameterTypes()));
            }
        }

        LOG.debug(methodMap.toString());

        return methodMap;
    }

    @Override
    public void reset() {
        super.reset();
        serviceClass = null;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        if (VariationsEnhancerHelper.isEnhancedClass(serviceClass)) {
            reset();
            this.serviceClass = serviceClass;
            try {
                getOriginalInstantiationStrategy().setServiceClass(VariationsEnhancerHelper.undecorateMethods(serviceClass,
                    getClassLoader()));
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to set service class to enhancer. Failed to get undecorated class.",
                    e);
            }
        } else {
            throw new OpenlNotCheckedException("Failed to set service class to variations enhancer. Service class shoud have at least one method with VariationsPack as the last parameter and with returnt type VariationsResult.");
        }
    }

    @Override
    public boolean isServiceClassDefined() {
        return true;
    }

    @Override
    public Class<?> getInstanceClass() throws ClassNotFoundException, RulesInstantiationException {
        return getServiceClass();
    }
}
