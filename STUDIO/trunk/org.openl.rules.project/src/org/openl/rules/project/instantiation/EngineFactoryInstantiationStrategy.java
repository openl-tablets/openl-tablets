package org.openl.rules.project.instantiation;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
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
    public static final String RULE_OPENL_NAME = "org.openl.xls";

    private EngineFactory<?> engineFactory;
    public EngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public EngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
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
    protected CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        EngineFactory<?> engineInstanceFactory = getEngineFactory(clazz);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());

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
    protected Object instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        EngineFactory<?> engineInstanceFactory = getEngineFactory(clazz);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());

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
