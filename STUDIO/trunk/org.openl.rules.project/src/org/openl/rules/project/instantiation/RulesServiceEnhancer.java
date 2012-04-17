package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;

/**
 * Auxiliary class which enhances rule service with ability to use rule service
 * method with rules runtime context during method invocation. The class is used
 * by engine to expose rules services as web service.
 * 
 * Enhancer class decorates methods of original service class and exposes new
 * methods signatures instead of original methods. New method signature has one
 * more parameter - rules runtime context. While service method invocation
 * engine do the following steps:
 * <ul>
 * <li>recognize context parameter;</li>
 * <li>recognize original service method to invoke using method signature;</li>
 * <li>set rules runtime context;</li>
 * <li>invoke appropriate service method.</li>
 * </ul>
 */
public class RulesServiceEnhancer extends RulesInstantiationStrategyDelegator {
    
    private static final Log LOG = LogFactory.getLog(RulesServiceEnhancer.class);


    /**
     * Internal generated class at runtime which used as service class.
     */
    private Class<?> serviceClass;
    
    /**
     * Constructs new instance of enhancer.
     * 
     * @param instantiationStrategy instantiation strategy which used to
     *            instantiate original service
     */
    public RulesServiceEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    /**
     * Gets enhanced service class.
     * 
     * @return service class
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    public Class<?> getServiceClass() throws ClassNotFoundException {
        if (serviceClass == null) {
            try {
                Class<?> originalServiceClass = getOriginalInstantiationStrategy().getInstanceClass();
                serviceClass = RulesServiceEnhancerHelper.decorateMethods(originalServiceClass, getClassLoader());
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to add runtime context in parameters of each method.", e);
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
            throw new RulesInstantiationException(e.getMessage(),e);
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
        return new RulesServiceEnhancerInvocationHandler(methodsMap, getOriginalInstantiationStrategy().instantiate());
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
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) {

        LOG.debug(String.format("Creating methods map for classes: %s <-> %s", interfaceClass, serviceClass));

        Map<Method, Method> methodMap = new HashMap<Method, Method>();
        Method[] serviceMethods = serviceClass.getDeclaredMethods();

        for (Method serviceMethod : serviceMethods) {

            String interfaceMethodName = serviceMethod.getName();
            Class<?>[] serviceMethodParameterTypes = serviceMethod.getParameterTypes();

            Class<?>[] newParams = new Class<?>[] { IRulesRuntimeContext.class };
            Class<?>[] extendedParamTypes = (Class<?>[]) ArrayUtils.addAll(newParams, serviceMethodParameterTypes);

            Method interfaceMethod;
            try {
                interfaceMethod = interfaceClass.getMethod(interfaceMethodName, extendedParamTypes);
                methodMap.put(interfaceMethod, serviceMethod);
            } catch (NoSuchMethodException e) {
                // Ignore an exception. Interface class can ignore several
                // methods what declared in service class.
                //
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
        if (RulesServiceEnhancerHelper.isEnhancedClass(serviceClass)) {
            reset();
            this.serviceClass = serviceClass;
            try {
                getOriginalInstantiationStrategy().setServiceClass(RulesServiceEnhancerHelper.undecorateMethods(serviceClass,
                    getOriginalInstantiationStrategy().getClassLoader()));
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to set service class to enhancer. Failed to get undecorated class.",
                    e);
            }
        } else {
            throw new OpenlNotCheckedException("Failed to set service class to enhancer. Service shoud have IRulesRuntimeContext as the first argument of each method.");
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
