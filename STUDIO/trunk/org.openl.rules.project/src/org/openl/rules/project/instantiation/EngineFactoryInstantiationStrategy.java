package org.openl.rules.project.instantiation;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.runtime.EngineFactory;

import java.io.File;

/**
 * Instantiation strategy for projects with interface. Generates proxy for
 * interface by Excel file.
 * 
 * @author PUdalau
 */
public class EngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    public static final String RULE_OPENL_NAME = "org.openl.xls";

    private EngineFactory<?> engineFactoryInstance;
    public EngineFactoryInstantiationStrategy(Module module) {
        super(module);
    }

    @SuppressWarnings("unchecked")
    private EngineFactory<?> getEngineFactory(Class<?> clazz) {
        if(engineFactoryInstance == null){
            File sourceFile = new File(getModule().getProject().getProjectFolder(), getModule().getRulesRootPath()
                    .getPath());
            engineFactoryInstance = new EngineFactory(RULE_OPENL_NAME, sourceFile, clazz);
        }
        return engineFactoryInstance;
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
