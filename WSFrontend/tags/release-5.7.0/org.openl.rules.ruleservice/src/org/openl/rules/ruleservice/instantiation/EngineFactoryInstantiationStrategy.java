package org.openl.rules.ruleservice.instantiation;

import org.openl.runtime.EngineFactory;

import java.io.File;

public class EngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    public static final String RULE_OPENL_NAME = "org.openl.xls";
    
    private File sourceFile;

    public EngineFactoryInstantiationStrategy(File sourceFile, Class<?> rulesInterfaceClass) {
        super(rulesInterfaceClass);
        this.sourceFile = sourceFile;
    }
    
    public EngineFactoryInstantiationStrategy(File sourceFile, String rulesInterfaceClassName, ClassLoader classLoader) {
        super(rulesInterfaceClassName, classLoader);
        this.sourceFile = sourceFile;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        EngineFactory<?> engineInstanceFactory = new EngineFactory(RULE_OPENL_NAME, sourceFile, clazz);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());

        try {
            return engineInstanceFactory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
