package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenlNotCheckedException;

/**
 * 
 * @author Marat Kamalov
 * 
 */
public abstract class AbstractServiceClassEnhancerInstantiationStrategy extends RulesInstantiationStrategyDelegator {

    /**
     * Internal generated class at runtime which used as service class.
     */
    private Class<?> serviceClass;

    public AbstractServiceClassEnhancerInstantiationStrategy(RulesInstantiationStrategy instantiationStrategy) {
        super(instantiationStrategy);
    }

    protected abstract Class<?> decorateServiceClass(Class<?> serviceClass, ClassLoader classLoader);

    protected abstract Class<?> undecorateServiceClass(Class<?> serviceClass, ClassLoader classLoader);

    protected abstract boolean validateServiceClass(Class<?> serviceClass) throws ValidationServiceClassException;

    /**
     * Gets enhanced service class.
     * 
     * @return service class
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    @Override
    public final Class<?> getServiceClass() throws ClassNotFoundException {
        if (serviceClass == null) {
            try {
                Class<?> originalServiceClass = getOriginalInstantiationStrategy().getInstanceClass();
                serviceClass = decorateServiceClass(originalServiceClass, getClassLoader());
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to enhance service class.", e);
            }
        }
        return serviceClass;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        try {
            if (validateServiceClass(serviceClass)) {
                // FIX IT
                if (classLoader != null) {
                    try {
                        classLoader.addClassLoader(initClassLoader());
                    } catch (RulesInstantiationException e) {
                        throw new OpenlNotCheckedException(e.getMessage(), e);
                    }
                }

                this.serviceClass = serviceClass;
                try {
                    Class<?> clazz = undecorateServiceClass(serviceClass,
                        getOriginalInstantiationStrategy().getClassLoader());
                    getOriginalInstantiationStrategy().setServiceClass(clazz);
                } catch (Exception e) {
                    throw new OpenlNotCheckedException("Failed to set service class to instantiation strategy enhancer. Failed to get undecorated class.",
                        e);
                }
            } else {
                throw new OpenlNotCheckedException("Failed to set service class to instantiation strategy enhancer. Service class isn't supported by this strategy!");
            }
        } catch (ValidationServiceClassException e) {
            throw new OpenlNotCheckedException("Failed to set service class to instantiation strategy enhancer. Service class isn't supported by this strategy!",
                e);
        }
    }

    /**
     * Makes invocation handler.
     * 
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    protected abstract InvocationHandler makeInvocationHandler(Object instanceObject) throws Exception;

    /**
     * Gets interface classes what used for proxy construction.
     * 
     * @return proxy interfaces
     * @throws Exception
     */
    protected Class<?>[] getProxyInterfaces(Object originalInstance) throws Exception {
        List<Class<?>> proxyInterfaces = new ArrayList<Class<?>>();
        proxyInterfaces.add(getServiceClass());
        Class<?> originalServiceClass = getOriginalInstantiationStrategy().getInstanceClass();
        for (Class<?> interfaceClass : originalInstance.getClass().getInterfaces()) {
            if (!interfaceClass.equals(originalServiceClass)) {
                proxyInterfaces.add(interfaceClass);
            }
        }
        return proxyInterfaces.toArray(new Class<?>[] {});
    }

    @Override
    public final Object instantiate() throws RulesInstantiationException, ClassNotFoundException {
        try {
            Object originalInstance = getOriginalInstantiationStrategy().instantiate();
            InvocationHandler invocationHandler = makeInvocationHandler(originalInstance);
            return Proxy.newProxyInstance(getClassLoader(), getProxyInterfaces(originalInstance), invocationHandler);
        } catch (Exception e) {
            throw new RulesInstantiationException(e.getMessage(), e);
        }
    }

    @Override
    public final boolean isServiceClassDefined() {
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        serviceClass = null;
    }

    @Override
    public final Class<?> getInstanceClass() throws ClassNotFoundException, RulesInstantiationException {
        return getServiceClass();
    }
}
