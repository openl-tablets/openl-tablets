package org.openl.rules.project.instantiation;

import java.io.File;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;

public class ApiBasedEngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    private Object instance;
    private CompiledOpenClass compiledOpenClass;
    private Class<?> serviceClass;

    public ApiBasedEngineFactoryInstantiationStrategy(Module module) {
        super(module);
        init();
    }

    private void init() {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            File sourceFile = new File(getModule().getRulesRootPath().getPath());
            ApiBasedRulesEngineFactory factory = new ApiBasedRulesEngineFactory(sourceFile);
            instance = factory.makeInstance();
            serviceClass = factory.getInterfaceClass();
            compiledOpenClass = factory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    @Override
    protected CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        return compiledOpenClass;
    }

    @Override
    protected Object instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        return instance;
    }

}
