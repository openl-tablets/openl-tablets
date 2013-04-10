package org.openl.rules.ruleservice.publish.cache;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStartegy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.BaseRulesFactory;

/**
 * Prebinds multimodule openclass and creates LazyMethod and LazyField that will
 * compile neccessary modules on demand.
 * 
 * @author pudalau
 */
public class LazyMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {
	
    private final Log log = LogFactory.getLog(LazyMultiModuleInstantiationStrategy.class);

    private LazyMultiModuleEngineFactory factory;
    private String openlName;
    
    
    public String getOpenlName() {
		return openlName;
	}

	public void setOpenlName(String openlName) {
		this.openlName = openlName;
	}

	public LazyMultiModuleInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager, String openlName) {
        super(modules, dependencyManager);
        this.openlName = openlName;
    }

    @Override
    public void reset() {
        super.reset();
        factory = null;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Cannot resolve interface", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException{
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException{

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private LazyMultiModuleEngineFactory getEngineFactory() {
    	Class<?> serviceClass = null;
    	try {
			serviceClass = getServiceClass();
		} catch (ClassNotFoundException e) {
			log.debug("Failed to get service class.", e);
			serviceClass = null;
		}
		if (factory == null
				|| (serviceClass != null && !factory.getInterfaceClass()
						.equals(serviceClass))) {
            factory = new LazyMultiModuleEngineFactory(getModules(), openlName);

            //Information for interface generation, if generation required.
            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<String>();
            Collection<String> allExcludes = new HashSet<String>();
            for (Module m : getModules()){
                MethodFilter methodFilter = m.getMethodFilter();
                allIncludes.addAll(methodFilter.getIncludes());
                allExcludes.addAll(methodFilter.getExcludes());
            }
            if (!allIncludes.isEmpty() || !allExcludes.isEmpty()) {
                String[] includes = new String[]{};
                String[] excludes = new String[]{};
                includes = allIncludes.toArray(includes); 
                excludes = allExcludes.toArray(excludes); 
                factory.setRulesFactory(new BaseRulesFactory(includes, excludes));
            }
            factory.setDependencyManager(getDependencyManager());
            factory.setExternalParameters(getExternalParameters());
            factory.setInterfaceClass(serviceClass);
        }

        return factory;
    }
}