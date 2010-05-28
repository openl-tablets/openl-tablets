package org.openl.rules.ruleservice.instantiation;

import org.openl.runtime.EngineFactory;

import java.io.File;

public class EngineFactoryInstantiationStrategy extends AClassInstantiationStrategy {
    private File sourceFile;

    public EngineFactoryInstantiationStrategy(File sourceFile, String serviceClassName, ClassLoader classLoader) {
        super(serviceClassName, classLoader);
        this.sourceFile = sourceFile;
    }

    @Override
    protected Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {

        EngineFactory f = new EngineFactory("org.openl.xls", sourceFile, clazz);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());

        try {
            return f.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
