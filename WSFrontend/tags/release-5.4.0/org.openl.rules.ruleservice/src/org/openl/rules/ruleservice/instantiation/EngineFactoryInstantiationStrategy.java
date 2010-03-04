package org.openl.rules.ruleservice.instantiation;

import org.openl.runtime.EngineFactory;

import java.io.File;

public class EngineFactoryInstantiationStrategy implements InstantiationStrategy {
    private File sourceFile;

    public EngineFactoryInstantiationStrategy(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        EngineFactory f = new EngineFactory("org.openl.xls", sourceFile, clazz);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            return f.newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
