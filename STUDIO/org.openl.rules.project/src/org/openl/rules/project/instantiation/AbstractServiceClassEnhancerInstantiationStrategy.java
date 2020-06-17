package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.ASMProxyHandler;

/**
 *
 * @author Marat Kamalov
 *
 */
public abstract class AbstractServiceClassEnhancerInstantiationStrategy implements RulesInstantiationStrategy {

    /**
     * Instantiation strategy delegate.
     */
    private final RulesInstantiationStrategy instantiationStrategy;

    /**
     * Internal generated class at runtime which used as service class.
     */
    private Class<?> serviceClass;

    public AbstractServiceClassEnhancerInstantiationStrategy(RulesInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    protected abstract Class<?> decorateServiceClass(Class<?> serviceClass, ClassLoader classLoader);

    protected abstract Class<?> undecorateServiceClass(Class<?> serviceClass, ClassLoader classLoader);

    protected abstract boolean validateServiceClass(Class<?> serviceClass) throws ValidationServiceClassException;

    /**
     * Gets enhanced service class.
     *
     * @return service class
     */
    @Override
    public final Class<?> getServiceClass() {
        if (serviceClass == null) {
            try {
                Class<?> originalServiceClass = getOriginalInstantiationStrategy().getInstanceClass();
                serviceClass = decorateServiceClass(originalServiceClass, getClassLoader());
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to enhance a service class.", e);
            }
        }
        return serviceClass;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        try {
            if (validateServiceClass(serviceClass)) {
                this.serviceClass = serviceClass;
                try {
                    Class<?> clazz = undecorateServiceClass(serviceClass, getClassLoader());
                    getOriginalInstantiationStrategy().setServiceClass(clazz);
                } catch (Exception e) {
                    throw new OpenlNotCheckedException(
                        "Failed to set service class to instantiation strategy enhancer. Failed to get undecorated class.",
                        e);
                }
            } else {
                throw new OpenlNotCheckedException(
                    "Failed to set service class to instantiation strategy enhancer. Service class is not supported by this strategy.");
            }
        } catch (ValidationServiceClassException e) {
            throw new OpenlNotCheckedException(
                "Failed to set service class to instantiation strategy enhancer. Service class is not supported by this strategy.",
                e);
        }
    }

    /**
     * Makes invocation handler.
     *
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    protected abstract ASMProxyHandler makeMethodHandler(Object instanceObject) throws Exception;

    /**
     * Gets interface classes what used for proxy construction.
     *
     * @return proxy interfaces
     * @throws Exception
     */
    protected Class<?>[] getProxyInterfaces(Object originalInstance) throws Exception {
        List<Class<?>> proxyInterfaces = new ArrayList<>();
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
    public final Object instantiate() throws RulesInstantiationException {
        try {
            Object originalInstance = getOriginalInstantiationStrategy().instantiate();
            return ASMProxyFactory.newProxyInstance(getClassLoader(), makeMethodHandler(originalInstance), getProxyInterfaces(originalInstance));
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
        getOriginalInstantiationStrategy().reset();
        serviceClass = null;
    }

    @Override
    public ClassLoader getClassLoader() throws RulesInstantiationException {
        return getOriginalInstantiationStrategy().getInstanceClass().getClassLoader();
    }

    @Override
    public final Class<?> getInstanceClass() {
        return getServiceClass();
    }

    protected RulesInstantiationStrategy getOriginalInstantiationStrategy() {
        return instantiationStrategy;
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        return getOriginalInstantiationStrategy().compile();
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        return getOriginalInstantiationStrategy().getGeneratedRulesClass();
    }

    @Override
    public void forcedReset() {
        reset();
        getOriginalInstantiationStrategy().forcedReset();
    }

    @Override
    public Collection<Module> getModules() {
        return getOriginalInstantiationStrategy().getModules();
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return getOriginalInstantiationStrategy().getExternalParameters();
    }

    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        getOriginalInstantiationStrategy().setExternalParameters(parameters);
    }
}
