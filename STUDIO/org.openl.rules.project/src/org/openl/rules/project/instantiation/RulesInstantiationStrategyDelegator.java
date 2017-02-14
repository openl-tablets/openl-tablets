package org.openl.rules.project.instantiation;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.rules.project.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Instantiation strategy that is wrapper for another instantiation strategy.
 *
 * @author PUdalau
 */
public abstract class RulesInstantiationStrategyDelegator implements RulesInstantiationStrategy {

    private final Logger log = LoggerFactory.getLogger(RulesInstantiationStrategyDelegator.class);

    /**
     * Instantiation strategy delegate.
     */
    private RulesInstantiationStrategy instantiationStrategy;

    protected OpenLBundleClassLoader classLoader;

    public RulesInstantiationStrategyDelegator(RulesInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        return instantiationStrategy.compile();
    }

    @Override
    public ClassLoader getClassLoader() throws RulesInstantiationException {
        if (classLoader == null) {
            classLoader = initClassLoader();
        }

        return classLoader;
    }

    protected OpenLBundleClassLoader initClassLoader() throws RulesInstantiationException {
        ClassLoader parentClassLoader = RulesInstantiationStrategyDelegator.class.getClassLoader();
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(parentClassLoader);
        ClassLoader originalClassLoader = getOriginalInstantiationStrategy().getClassLoader();
        classLoader.addClassLoader(originalClassLoader);
        try {
            Class<?> serviceClass = instantiationStrategy.getServiceClass();
            if (serviceClass != null) {
                classLoader.addClassLoader(serviceClass.getClassLoader());
            }
        } catch (Exception e) {
            log.warn("Failed to register class loader of service class in class loader of Enhancer.", e);
        }
        return classLoader;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        return instantiationStrategy.getGeneratedRulesClass();
    }

    @Override
    public void reset() {
        instantiationStrategy.reset();
        classLoader = null;
    }

    public RulesInstantiationStrategy getOriginalInstantiationStrategy() {
        return instantiationStrategy;
    }

    @Override
    public void forcedReset() {
        reset();
        instantiationStrategy.forcedReset();
    }

    @Override
    public Collection<Module> getModules() {
        return instantiationStrategy.getModules();
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return instantiationStrategy.getExternalParameters();
    }

    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        instantiationStrategy.setExternalParameters(parameters);
    }
}
