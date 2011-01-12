package org.openl.rules.project.instantiation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;

import java.io.File;

/**
 * Instantiation strategy for projects with interface. Generates proxy for
 * interface by Excel file.
 * 
 * @author PUdalau
 */
public class EngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    
    private static final Log LOG = LogFactory.getLog(EngineFactoryInstantiationStrategy.class);
    
    public static final String RULE_OPENL_NAME = "org.openl.xls";

    private EngineFactory<?> engineFactory;
    
    public EngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public EngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
    }
    
    @Override
    public Class<?> getServiceClass() throws ClassNotFoundException {
        // Service class for current implementation will be interface provided by user.
        //
        if (getRulesClass() == null) {
            // Load rules interface and set it to strategy.
            setRulesInterface(getClassLoader().loadClass(getModule().getClassname()));
        }
        return getRulesClass();
    }

    @SuppressWarnings("unchecked")
    private EngineFactory<?> getEngineFactory(Class<?> clazz) {
        if(engineFactory == null){
            File sourceFile = new File(getModule().getProject().getProjectFolder(), getModule().getRulesRootPath()
                    .getPath());
            
            IOpenSourceCodeModule source = new FileSourceCodeModule(sourceFile, null);
            source.setParams(getModule().getProperties());

            engineFactory = new RuleEngineFactory(source, clazz);
            engineFactory.setExecutionMode(isExecutionMode());
            engineFactory.setDependencyManager(getDependencyManager());
        }
        
        return engineFactory;
    }
    
    @Override
    protected CompiledOpenClass compile(boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        try {
            return compile(getServiceClass(), useExisting);
        } catch (ClassNotFoundException e) {
            String errorMessage = String.format("Cannot find service class for %s", getModule().getClassname());
            LOG.error(errorMessage, e);
            throw new OpenlNotCheckedException(errorMessage, e);            
        }
    }

    private CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        EngineFactory<?> engineInstanceFactory = getEngineFactory(clazz);
        
        // Ensure that compilation will be done in strategy classLoader
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            if (!useExisting) {
                engineInstanceFactory.reset();
            }
            return engineInstanceFactory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected Object instantiate(Class<?> rulesClass, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        EngineFactory<?> engineInstanceFactory = getEngineFactory(rulesClass);
        
        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            if (!useExisting) {
                engineInstanceFactory.reset();
            }
            return engineInstanceFactory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
